/*
 *  Copyright (c) 2022 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.pragmatica.dns;

import org.pragmatica.dns.io.MessageType;
import org.pragmatica.dns.io.QuestionRecord;
import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.Timeout;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.net.InetAddress.Inet4Address;
import org.pragmatica.io.async.net.InetPort;
import org.pragmatica.io.async.net.SocketAddress;
import org.pragmatica.io.async.util.ActionableThreshold;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.PromiseIO;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;

import static org.pragmatica.dns.io.DnsIoErrors.NO_RESULTS_FOUND;
import static org.pragmatica.lang.PromiseIO.udpSocket;

public class DnsResolver {
    private static final InetPort DNS_PORT = InetPort.inetPort(53);

    private static final Inet4Address[] SERVERS = {
        new Inet4Address(new byte[]{1, 0, 0, 1}),                               // Cloudflare
        new Inet4Address(new byte[]{1, 1, 1, 1}),                               // Cloudflare
        new Inet4Address(new byte[]{8, 8, 4, 4}),                               // Google Public DNS
        new Inet4Address(new byte[]{8, 8, 8, 8}),                               // Google Public DNS
        new Inet4Address(new byte[]{9, 9, 9, 9}),                               // Quad9
        new Inet4Address(new byte[]{(byte) 149, 112, 112, 112}),                // Quad9
        new Inet4Address(new byte[]{(byte) 208, 67, (byte) 220, (byte) 220}),   // Cisco OpenDNS
        new Inet4Address(new byte[]{(byte) 208, 67, (byte) 222, (byte) 222}),   // Cisco OpenDNS
    };

    private record TtlEntry(DomainAddress domainAddress, long expirationTime) {
        static TtlEntry create(DomainAddress domainAddress) {
            return new TtlEntry(domainAddress, System.nanoTime() + domainAddress.ttl().toNanos());
        }
    }

    private final ConcurrentMap<DomainName, Promise<DomainAddress>> cache = new ConcurrentHashMap<>();
    private final List<FileDescriptor> sockets;
    private final PriorityBlockingQueue<TtlEntry> queue = new PriorityBlockingQueue<>(1024, Comparator.comparingLong(TtlEntry::expirationTime));

    private DnsResolver(List<Inet4Address> serverList) {
        this.sockets = serverList
                             .stream()
                             .map(inetAddress -> SocketAddress.socketAddress(DNS_PORT, inetAddress))
                             .map(address -> udpSocket().flatMap(fd -> PromiseIO.connect(fd, address)).join())
                             .map(Result::unwrap)
                             .toList();

        Promise.promise().async(this::ttlProcessor);
    }

    public static DnsResolver resolver() {
        return new DnsResolver(List.of(SERVERS));
    }

    public static DnsResolver resolver(List<Inet4Address> servers) {
        return new DnsResolver(servers);
    }

    public Promise<DomainAddress> forName(DomainName domainName) {
        return cache.computeIfAbsent(domainName, this::resolveDomain);
    }

    public Promise<Unit> stop() {
        var result = Promise.<Unit>promise();
        var threshold = ActionableThreshold.threshold(sockets.size(), () -> result.resolve(Unit.unitResult()));

        sockets.forEach(fd -> PromiseIO.close(fd).onResultDo(threshold::registerEvent));
        return result;
    }

    private Promise<DomainAddress> resolveDomain(DomainName domainName) {
        return Promise.<DomainAddress>promise().async(promise -> startResolve(promise, domainName));
    }

    @SuppressWarnings("resource")
    private void startResolve(Promise<DomainAddress> promise, DomainName domainName) {
        var requestBuffer = encodeQuery(domainName);

        Promise.anySuccess(NO_RESULTS_FOUND.result(),
                           sockets.stream()
                                  .map(socket -> querySingleServer(socket, requestBuffer, domainName))
                                  .toList())
               .onResult(promise::resolve)
               .onResultDo(requestBuffer::close);

        promise
            .onSuccess(domainAddress -> queue.offer(TtlEntry.create(domainAddress)));
    }

    private OffHeapSlice encodeQuery(DomainName domainName) {
        var requestBuffer = OffHeapSlice.fixedSize(4096);

        DnsMessageBuilder.create()
                         .messageType(MessageType.QUERY)
                         .questionRecord(QuestionRecord.addressV4ByName(domainName.name()))
                         .build()
                         .encode(requestBuffer);

        return requestBuffer;
    }

    @SuppressWarnings("resource")
    private Promise<DomainAddress> querySingleServer(FileDescriptor socket, OffHeapSlice requestBuffer, DomainName domainName) {
        var promise = Promise.<DomainAddress>promise();
        var responseBuffer = OffHeapSlice.fixedSize(4096);

        PromiseIO.write(socket, requestBuffer)
                 .flatMap(() -> PromiseIO.read(socket, responseBuffer))
                 .onResultDo(() -> decodeResponse(responseBuffer, promise, domainName))
                 .onResultDo(responseBuffer::close);

        return promise;
    }

    private void decodeResponse(OffHeapSlice buffer, Promise<DomainAddress> promise, DomainName domainName) {
        var domainAddress = DnsMessage.decode(buffer)
                                      .map(message -> message
                                          .answerRecords().stream()
                                          .map(ResourceRecord::toDomainAddress)
                                          .filter(Result::isSuccess)
                                          .map(Result::unwrap)
                                          .map(address -> address.replaceDomain(domainName))
                                          .findFirst())
                                      .flatMap(optional -> optional.map(Result::success).orElseGet(NO_RESULTS_FOUND::result));
        promise.resolve(domainAddress);
    }

    private void ttlProcessor(Object unused, Proactor proactor) {
        while (true) {
            var head = queue.peek();

            if (head == null) {
                break;
            }

            var time = System.nanoTime();

            if (head.expirationTime > time) {
                break;
            }

            head = queue.poll();

            if (head == null) {
                break;
            }

            cache.remove(head.domainAddress.name());
        }
        proactor.delay(this::ttlProcessor, Timeout.timeout(1).seconds());
    }
}

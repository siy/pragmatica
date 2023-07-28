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

import org.pragmatica.dns.codec.MessageType;
import org.pragmatica.dns.codec.QuestionRecord;
import org.pragmatica.io.AsyncCloseable;
import org.pragmatica.io.PromiseIO;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.net.InetAddress.Inet4Address;
import org.pragmatica.io.async.net.InetPort;
import org.pragmatica.io.async.util.ActionableThreshold;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.io.util.PeriodicTaskRunner;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;

import static java.util.Comparator.comparingLong;
import static org.pragmatica.dns.codec.DnsIoErrors.NO_RESULTS_FOUND;
import static org.pragmatica.io.async.Timeout.timeout;
import static org.pragmatica.io.async.net.SocketAddress.socketAddress;
import static org.pragmatica.lang.Functions.FN1.id;
import static org.pragmatica.lang.Promise.all;
import static org.pragmatica.lang.Promise.anySuccess;
import static org.pragmatica.lang.Unit.unitResult;

public class DomainNameResolver implements AsyncCloseable {
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
    }

    private final ConcurrentMap<DomainName, Promise<DomainAddress>> cache = new ConcurrentHashMap<>();
    private final List<FileDescriptor> sockets;
    private final PriorityBlockingQueue<TtlEntry> queue = new PriorityBlockingQueue<>(1024, comparingLong(TtlEntry::expirationTime));
    private final PeriodicTaskRunner taskRunner = PeriodicTaskRunner.periodicTaskRunner(timeout(1).seconds(), this::ttlProcessor);

    private DomainNameResolver(List<Inet4Address> serverList) {
        this.sockets = serverList
            .stream()
            .map(inetAddress -> socketAddress(DNS_PORT, inetAddress))
            .map(address -> PromiseIO.udpSocket().flatMap(fd -> PromiseIO.connect(fd, address)).join())
            .filter(Result::isSuccess)
            .map(res -> res.fold(err -> null, id()))
            .toList();

        if (sockets.isEmpty()) {
            throw new IllegalStateException("No DNS servers available");
        }

        taskRunner.start();
    }

    public static DomainNameResolver resolver() {
        return new DomainNameResolver(List.of(SERVERS));
    }

    public static DomainNameResolver resolver(List<Inet4Address> servers) {
        return new DomainNameResolver(servers);
    }

    public Promise<DomainAddress> forName(String domainName) {
        return forName(DomainName.fromString(domainName));
    }

    public Promise<DomainAddress> forName(DomainName domainName) {
        return cache.computeIfAbsent(domainName, this::resolveDomain);
    }

    @Override
    public Promise<Unit> close() {
        var runnerPromise = taskRunner.stop();
        var socketPromise = Promise.<Unit>promise();
        var threshold = ActionableThreshold.threshold(sockets.size(), () -> socketPromise.resolve(unitResult()));

        sockets.forEach(fd -> PromiseIO.close(fd).onResultDo(threshold::registerEvent));

        return all(runnerPromise, socketPromise)
            .map(Unit::unit);
    }

    private Promise<DomainAddress> resolveDomain(DomainName domainName) {
        return Promise.promise(promise -> startResolve(promise, domainName));
    }

    private void startResolve(Promise<DomainAddress> promise, DomainName domainName) {
        var requestBuffer = encodeQuery(domainName);

        anySuccess(NO_RESULTS_FOUND.result(),
                   sockets.stream()
                          .map(socket -> querySingleServer(socket, requestBuffer, domainName))
                          .toList())
            .onResult(promise::resolve)
            .onResultDo(requestBuffer::close);

        promise
            .onSuccess(domainAddress -> queue.offer(ttlEntry(domainAddress)));
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
        promise.resolve(decodeDomainAddress(buffer, domainName));
    }

    private Result<DomainAddress> decodeDomainAddress(OffHeapSlice buffer, DomainName domainName) {
        return DnsMessage.decode(buffer)
                         .flatMap(message -> decodeMessage(message, domainName));
    }

    private Result<DomainAddress> decodeMessage(DnsMessage message, DomainName domainName) {
        return message
            .answerRecords()
            .stream()
            .map(ResourceRecord::toDomainAddress)
            .filter(Result::isSuccess)
            .map(res -> res.fold(err -> null, id()))
            .map(address -> address.replaceDomain(domainName))
            .findFirst()
            .map(Result::success)
            .orElseGet(NO_RESULTS_FOUND::result);
    }

    private static TtlEntry ttlEntry(DomainAddress domainAddress) {
        return new TtlEntry(domainAddress, System.nanoTime() + domainAddress.ttl().toNanos());
    }

    private void ttlProcessor() {
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
    }
}

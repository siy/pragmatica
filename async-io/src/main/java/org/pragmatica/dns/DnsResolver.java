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

import org.pragmatica.dns.io.DnsIoErrors;
import org.pragmatica.dns.io.MessageType;
import org.pragmatica.dns.io.QuestionRecord;
import org.pragmatica.dns.io.RecordClass;
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

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.pragmatica.dns.io.DnsIoErrors.NO_RESULTS_FOUND;
import static org.pragmatica.lang.PromiseIO.udpSocket;
import static org.pragmatica.lang.Tuple.tuple;

public class DnsResolver {
    private static final InetPort DNSPORT = InetPort.inetPort(53);
    // TODO: pass list as a parameter
    private static final Inet4Address[] SERVERS = {
        new Inet4Address(new byte[]{8, 8, 8, 8}),   // Google Public DNS
        new Inet4Address(new byte[]{8, 8, 4, 4}), // Google Public DNS

//        new Inet4Address(new byte[]{208, 67, 222, 222}),  //Cisco OpenDNS
//        new Inet4Address(new byte[]{208, 67, 220, 220}),//Cisco OpenDNS
        new Inet4Address(new byte[]{1, 1, 1, 1}), //Cloudflare
        new Inet4Address(new byte[]{1, 0, 0, 1}),          //Cloudflare
        new Inet4Address(new byte[]{9, 9, 9, 9}),  //Quad9
        //new Inet4Address(new byte[]{149, 112, 112, 112}), //Quad9
    };

    private final ConcurrentMap<DomainName, Promise<DomainAddress>> cache = new ConcurrentHashMap<>();
    private final List<FileDescriptor> sockets;

    private DnsResolver(List<FileDescriptor> sockets) {
        this.sockets = sockets;
    }

    public static DnsResolver resolver() {
        var list = Arrays.stream(SERVERS)
                         .map(inetAddress -> SocketAddress.socketAddress(DNSPORT, inetAddress))
                         .map(address -> udpSocket().flatMap(fd -> PromiseIO.connect(fd, address)).join())
                         .map(Result::unwrap)
                         .toList();

        return new DnsResolver(list);
    }

    public Promise<DomainAddress> ipForName(DomainName domainName) {
        return cache.computeIfAbsent(domainName, this::resolveDomain);
    }

    private Promise<DomainAddress> resolveDomain(DomainName domainName) {
        var promise = Promise.<DomainAddress>promise();

        promise.async(p -> startResolve(p, domainName));

        return promise;
    }

    @SuppressWarnings("resource")
    private void startResolve(Promise<DomainAddress> promise, DomainName domainName) {
        var requestBuffer = encodeQuery(domainName);

        Promise.anySuccess(NO_RESULTS_FOUND.result(),
                           sockets.stream().map(socket -> querySingleServer(socket, requestBuffer, domainName)).toList())
               .onResult(promise::resolve)
               .onResultDo(requestBuffer::close);
        promise
            .onSuccess(domainAddress -> setupTimer(domainName, domainAddress.ttl()));
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

    //TODO: use common 1s timer
    private void setupTimer(DomainName domainName, Duration ttl) {
        PromiseIO.delay(Timeout.fromDuration(ttl)).onResultDo(() -> cache.remove(domainName));
    }

    @SuppressWarnings("resource")
    private Promise<DomainAddress> querySingleServer(FileDescriptor socket, OffHeapSlice requestBuffer, DomainName domainName) {
        var promise = Promise.<DomainAddress>promise();
        var responseBuffer = OffHeapSlice.fixedSize(4096);

        PromiseIO.write(socket, requestBuffer)
                 .flatMap(__ -> PromiseIO.read(socket, responseBuffer))
                 .onResult(__ -> decodeResponse(responseBuffer, promise, domainName))
                 .onResultDo(responseBuffer::close);

        return promise;
    }

    private void decodeResponse(OffHeapSlice buffer, Promise<DomainAddress> promise, DomainName domainName) {
        var domainAddress = DnsMessage.decode(buffer)
                                      .map(message -> message
                                          .answerRecords().stream()
                                          .filter(record -> record.recordClass() == RecordClass.IN)
                                          .filter(record -> record.recordType().isAddress())
                                          .map(record -> tuple(domainName, record.domainData().ip(), Duration.ofSeconds(record.ttl())))
                                          .map(tuple -> tuple.map(DomainAddress::domainAddress))
                                          .findFirst())
                                      .flatMap(optional -> optional.map(Result::success).orElseGet(NO_RESULTS_FOUND::result));
        promise.resolve(domainAddress);
    }

    public Promise<Unit> stop() {
        var result = Promise.<Unit>promise();
        var threshold = ActionableThreshold.threshold(sockets.size(), () -> result.resolve(Unit.unitResult()));

        sockets.forEach(fd -> PromiseIO.close(fd).onResultDo(threshold::registerEvent));
        return result;
    }
}

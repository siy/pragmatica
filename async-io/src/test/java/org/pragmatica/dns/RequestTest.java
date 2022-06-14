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

import org.junit.jupiter.api.Test;
import org.pragmatica.dns.codec.MessageType;
import org.pragmatica.dns.codec.QuestionRecord;
import org.pragmatica.dns.codec.RecordClass;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.net.*;
import org.pragmatica.io.async.util.OffHeapSlice;

import java.util.concurrent.atomic.AtomicReference;

import static org.pragmatica.io.async.net.InetPort.inetPort;
import static org.pragmatica.lang.Option.option;
import static org.pragmatica.lang.PromiseIO.*;
import static org.pragmatica.lang.PromiseIO.close;

public class RequestTest {
    private static final byte[] DNS1 = new byte[]{8, 8, 8, 8};

    @Test
    void makeRequest() {
        var socket = new AtomicReference<FileDescriptor>();
        var serverAddress = InetAddress.inet4Address(DNS1).unwrap();
        var socketAddress = SocketAddress.socketAddress(inetPort(53), serverAddress);

        var dnsQuery = DnsMessageBuilder.create()
                                        .messageType(MessageType.QUERY)
                                        .questionRecord(QuestionRecord.addressV4ByName("www.google.com"))
                                        .build();


        try (var query = OffHeapSlice.fixedSize(4096);
             var buffer = OffHeapSlice.fixedSize(4096)) {

            dnsQuery.encode(query);

            var result = socket(AddressFamily.INET, SocketType.DGRAM, SocketFlag.closeOnExec(), SocketOption.reuseAll())
                .onSuccess(socket::set)
                .flatMap(() -> connect(socket.get(), socketAddress))
                .flatMap(() -> write(socket.get(), query))
                .flatMap(() -> read(socket.get(), buffer))
                .join();

            record answer(String domainName, InetAddress inetAddress, int ttl) {}

            var domainIpAnswer = DnsMessage.decode(buffer)
                                           .map(message -> message
                                               .answerRecords().stream()
                                               .filter(record -> record.recordClass() == RecordClass.IN)
                                               .filter(record -> record.recordType().isAddress())
                                               .map(record -> new answer(record.domainName(), record.domainData().ip(), record.ttl()))
                                               .toList());

            System.out.println(result);
            System.out.println(domainIpAnswer);
        } finally {
            option(socket.get())
                .onPresent(fd -> close(fd).join());
        }
    }
}

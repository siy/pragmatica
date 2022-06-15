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

package org.pragmatica.io.util;

import org.junit.jupiter.api.Test;
import org.pragmatica.dns.DomainNameResolver;
import org.pragmatica.io.async.net.InetAddress;
import org.pragmatica.io.async.net.InetPort;
import org.pragmatica.lang.Promise;

import java.nio.charset.StandardCharsets;

class ReadWriteContextTest {
    private static final String MINIMAL_REQUEST = "GET / HTTP/1.1\r\nHost: %s\r\nConnection: close\r\n\r\n";
    private final DomainNameResolver resolver = DomainNameResolver.resolver();


    @Test
    void tryWriteThenRead() {
        writeReadHost("www.ibm.com");
        writeReadHost("www.google.com");
        writeReadHost("www.twitter.com");
    }

    private void writeReadHost(String host) {
        var result =
            resolver.forName(host)
                    .map(domainAddress -> domainAddress.clientTcpConnector(InetPort.inetPort(80)))
                    .flatMap(ClientConnector::connect)
                    .map(ReadWriteContext::readWriteContext)
                    .flatMap(context -> doConversation(context, host))
                    .flatMap(ReadWriteContext::close)
                    .join();

        System.out.println("\nResult: " + result + "\n");
    }

    private <T extends InetAddress> Promise<ReadWriteContext<T>> doConversation(ReadWriteContext<T> context, String host) {
        var request = String.format(MINIMAL_REQUEST, host).getBytes(StandardCharsets.UTF_8);

        return context.prepareThenWrite(writeAccessor -> writeAccessor.putBytes(request).updateSlice())
                      .flatMap(() -> context.readPlain(readAccessor -> new String(readAccessor.getRemainingBytes(), StandardCharsets.UTF_8)))
                      .onSuccess(System.out::println)
                      .onFailure(System.out::println)
                      .mapReplace(() -> context);
    }
}
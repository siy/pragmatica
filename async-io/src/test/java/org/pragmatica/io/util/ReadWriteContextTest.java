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
import org.pragmatica.dns.DomainName;
import org.pragmatica.dns.DomainNameResolver;
import org.pragmatica.io.async.net.InetAddress;
import org.pragmatica.io.async.net.InetPort;
import org.pragmatica.io.async.util.SliceAccessor;
import org.pragmatica.lang.Promise;

import java.nio.charset.StandardCharsets;

class ReadWriteContextTest {
    private static final String MINIMAL_REQUEST = "GET / HTTP/1.1\r\nHost: %s\r\nConnection: close\r\n\r\n";

    @Test
    void tryWriteThenRead() {
        writeReadHost("www.ibm.com");
    }

    private void writeReadHost(String host) {
        var resolver = DomainNameResolver.resolver();

        var result =
            resolver.forName(DomainName.fromString(host))
                    .map(domainAddress -> domainAddress.clientTcpConnector(InetPort.inetPort(80)))
                    .flatMap(ClientConnector::connect)
                    .map(context -> ReadWriteContext.readWriteContext(context, 16384))
                    .flatMap(context -> doConversation(context, host))
                    .flatMap(ReadWriteContext::close)
                    .flatMap(resolver::close)
                    .join();

        System.out.println("Result: " + result);
    }

    private <T extends InetAddress> Promise<ReadWriteContext<T>> doConversation(ReadWriteContext<T> context, String host) {
        var request = String.format(MINIMAL_REQUEST, host).getBytes(StandardCharsets.UTF_8);

        return context.write(buffer -> SliceAccessor.forSlice(buffer).putBytes(request).updateSlice())
                      .flatMap(() -> context.read(buffer -> new String(buffer.export(), StandardCharsets.UTF_8)))
                      .onSuccess(System.out::println)
                      .onFailure(System.out::println)
                      .mapReplace(() -> context);
    }
}
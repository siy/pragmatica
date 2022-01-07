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

package org.pfj.io.net.protocols;

import org.junit.jupiter.api.Test;
import org.pfj.io.net.Server;
import org.pfj.io.net.tcp.ServerConfig;

class EchoServerProtocolTest {
    @Test
    void serverCanBeStarted() {
        var config = ServerConfig.config().withPort(12345).build();
        var server = Server.tcp(config);

        Runtime.getRuntime()
               .addShutdownHook(server.shutdownHook());

        server.serve(EchoServerProtocol::echoServer)
              .onFailure(System.out::println)
              .flatMap(server::shutdown)
              .onFailure(System.out::println)
              .join();
    }
}
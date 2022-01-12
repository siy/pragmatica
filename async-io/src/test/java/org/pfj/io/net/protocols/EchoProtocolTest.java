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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.pfj.io.net.Listener;

import static org.pfj.io.net.AcceptProtocol.acceptProtocol;
import static org.pfj.io.net.tcp.ListenConfig.listenConfig;
import static org.pfj.lang.Option.empty;

//@Disabled
class EchoProtocolTest {
    @Test
    void serverCanBeStarted() {
        var listener = Listener.listener(listenConfig().withPort(12345).build());

        Runtime.getRuntime()
               .addShutdownHook(listener.shutdownHook());

        listener.listen(acceptProtocol(EchoProtocol.starter(4096, empty())))
              .onFailure(System.out::println)
              .flatMap(listener::shutdown)
              .onFailure(System.out::println)
              .join();
    }
}
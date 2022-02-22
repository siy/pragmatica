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

package org.pragmatica.io.net.protocols;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Cause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.pragmatica.io.async.net.InetAddress.Inet4Address.INADDR_ANY;
import static org.pragmatica.io.net.Listener.listener;
import static org.pragmatica.io.net.protocols.EchoProtocol.acceptEchoProtocol;
import static org.pragmatica.io.net.tcp.ListenConfig.listenConfig;
import static org.pragmatica.lang.Option.empty;

@Tag("Infinite")
class EchoProtocolTest {
    private static final Logger LOG = LoggerFactory.getLogger(EchoProtocolTest.class);

    @Test
    void serverCanBeStarted() {
        listener(listenConfig(acceptEchoProtocol(INADDR_ANY, 4096, empty()))
                     .withPort(12345)
                     .build())
            .listen()
            .onFailure(this::printFailure)
            .join();
    }

    private void printFailure(Cause cause) {
        LOG.warn("Error: {}", cause.message());
    }
}
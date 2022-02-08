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

package org.pragmatica.example.echo;

import org.pragmatica.io.net.Listener;
import org.pragmatica.io.net.protocols.EchoProtocol;
import org.pragmatica.lang.Cause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static org.pragmatica.io.net.AcceptProtocol.acceptProtocol;
import static org.pragmatica.io.net.tcp.ListenConfig.listenConfig;
import static org.pragmatica.lang.Option.empty;

public class EchoServer {
    private static final Logger LOG = LoggerFactory.getLogger(EchoServer.class);

    public static void main(String[] args) {
        var config = listenConfig().withPort(12345).build();
        var listener = Listener.listener(config);

        LOG.info("Starting server {}", config);
        listener.listen(acceptProtocol(EchoProtocol.starter(4096, empty())))
                .onFailure(EchoServer::printFailure)
                .flatMap(listener::shutdown)
                .onFailure(EchoServer::printFailure)
                .join();
    }

    private static void printFailure(Cause cause) {
        LOG.warn("Error: {}", cause);
    }
}

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

import org.pragmatica.io.async.net.InetAddress;
import org.pragmatica.io.async.net.InetPort;
import org.pragmatica.io.async.net.SocketAddress;
import org.pragmatica.io.async.net.SocketType;
import org.pragmatica.lang.Promise;

import static org.pragmatica.lang.io.PromiseIO.socket;

public class ClientConnector<T extends InetAddress> {
    private final ConnectorType type;
    private final SocketAddress<T> address;

    private ClientConnector(ConnectorType type, SocketAddress<T> address) {
        this.type = type;
        this.address = address;
    }

    public static <T extends InetAddress> ClientConnector<T> udpConnector(T address, InetPort port) {
        return new ClientConnector<>(ConnectorType.UDP, SocketAddress.genericAddress(port, address));
    }

    public static <T extends InetAddress> ClientConnector<T> tcpConnector(T address, InetPort port) {
        return new ClientConnector<>(ConnectorType.TCP, SocketAddress.genericAddress(port, address));
    }

    public static <T extends InetAddress> ClientConnector<T> connector(ConnectorType type, T address, InetPort port) {
        return new ClientConnector<>(type, SocketAddress.genericAddress(port, address));
    }

    public Promise<ClientConnectionContext<T>> connect() {
        return socket(type == ConnectorType.UDP ? SocketType.DGRAM : SocketType.STREAM)
            .flatMap(fd -> PromiseIO.connect(fd, address)).map(fd -> new ClientConnectionContext<>(address, fd));
    }
}

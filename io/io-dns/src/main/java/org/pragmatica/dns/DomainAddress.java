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

import org.pragmatica.io.async.net.InetAddress;
import org.pragmatica.io.async.net.InetPort;
import org.pragmatica.io.util.ClientConnector;
import org.pragmatica.io.util.ConnectorType;

import java.time.Duration;

public interface DomainAddress {
    DomainName name();
    InetAddress ip();
    Duration ttl();

    static DomainAddress domainAddress(DomainName name, InetAddress ip, Duration ttl) {
        record domainAddress(DomainName name, InetAddress ip, Duration ttl) implements DomainAddress {}

        return new domainAddress(name, ip, ttl);
    }

    default DomainAddress replaceDomain(DomainName domainName) {
        return domainAddress(domainName, ip(), ttl());
    }

    @SuppressWarnings("unchecked")
    default <T extends InetAddress> ClientConnector<T> clientConnector(ConnectorType type, InetPort port) {
        return ClientConnector.<T>connector(type, (T) ip(), port);
    }

    default <T extends InetAddress> ClientConnector<T> clientUdpConnector(InetPort port) {
        return clientConnector(ConnectorType.UDP, port);
    }

    default <T extends InetAddress> ClientConnector<T> clientTcpConnector(InetPort port) {
        return clientConnector(ConnectorType.TCP, port);
    }
}

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

package org.pfj.io.net.tcp;

import org.pfj.io.async.common.SizeT;
import org.pfj.io.async.net.*;

import java.util.Set;

import static org.pfj.io.async.common.SizeT.sizeT;
import static org.pfj.io.async.net.InetPort.inetPort;

//TODO: add support for IPv6
public interface ServerConfig {
    SocketAddress<?> address();

    Set<SocketFlag> listenerFlags();

    Set<SocketFlag> acceptorFlags();

    Set<SocketOption> listenerOptions();

    SizeT backlogSize();

    class ServerConfigBuilder {
        private Inet4Address address = Inet4Address.INADDR_ANY;
        private InetPort port = inetPort(8081);
        private Set<SocketFlag> listenerFlags = SocketFlag.closeOnExec();
        private Set<SocketFlag> acceptorFlags = SocketFlag.closeOnExec();
        private Set<SocketOption> listenerOptions = SocketOption.reuseAll();
        private SizeT backlogSize = sizeT(16);

        ServerConfigBuilder withAddress(Inet4Address address) {
            this.address = address;
            return this;
        }

        ServerConfigBuilder withPort(InetPort port) {
            this.port = port;
            return this;
        }

        ServerConfigBuilder withListenerFlags(Set<SocketFlag> listenerFlags) {
            this.listenerFlags = listenerFlags;
            return this;
        }

        ServerConfigBuilder withAcceptorFlags(Set<SocketFlag> acceptorFlags) {
            this.acceptorFlags = acceptorFlags;
            return this;
        }

        ServerConfigBuilder withListenerOptions(Set<SocketOption> listenerOptions) {
            this.listenerOptions = listenerOptions;
            return this;
        }

        ServerConfigBuilder withBacklogSize(SizeT backlogSize) {
            this.backlogSize = backlogSize;
            return this;
        }

        ServerConfig build() {
            record serverConfig(SocketAddress<?> address, Set<SocketFlag> listenerFlags,
                                Set<SocketFlag> acceptorFlags, Set<SocketOption> listenerOptions, SizeT backlogSize)
                implements ServerConfig {}

            return new serverConfig(SocketAddressIn.create(port, address), listenerFlags, acceptorFlags, listenerOptions, backlogSize);
        }
    }
}

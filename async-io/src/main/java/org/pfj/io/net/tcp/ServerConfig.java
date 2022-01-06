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

public interface ServerConfig<T extends InetAddress> {
    SocketAddress<T> address();

    Set<SocketFlag> listenerFlags();

    Set<SocketFlag> acceptorFlags();

    Set<SocketOption> listenerOptions();

    SizeT backlogSize();

    static ServerConfigBuilder<Inet4Address> config() {
        return config(Inet4Address.INADDR_ANY);
    }

    static ServerConfigBuilder<Inet4Address> config(Inet4Address address) {
        return new ServerConfigBuilder<>(address);
    }

    static ServerConfigBuilder<Inet6Address> config6() {
        return config6(Inet6Address.INADDR_ANY);
    }

    static ServerConfigBuilder<Inet6Address> config6(Inet6Address address) {
        return new ServerConfigBuilder<>(address);
    }

    class ServerConfigBuilder<T extends InetAddress> {
        private InetPort port = inetPort(8081);
        private Set<SocketFlag> listenerFlags = SocketFlag.closeOnExec();
        private Set<SocketFlag> acceptorFlags = SocketFlag.closeOnExec();
        private Set<SocketOption> listenerOptions = SocketOption.reuseAll();
        private SizeT backlogSize = sizeT(16);
        private final T address;

        private ServerConfigBuilder(T address) {
            this.address = address;
        }

        ServerConfigBuilder<T> withPort(InetPort port) {
            this.port = port;
            return this;
        }

        ServerConfigBuilder<T> withListenerFlags(Set<SocketFlag> listenerFlags) {
            this.listenerFlags = listenerFlags;
            return this;
        }

        ServerConfigBuilder<T> withAcceptorFlags(Set<SocketFlag> acceptorFlags) {
            this.acceptorFlags = acceptorFlags;
            return this;
        }

        ServerConfigBuilder<T> withListenerOptions(Set<SocketOption> listenerOptions) {
            this.listenerOptions = listenerOptions;
            return this;
        }

        ServerConfigBuilder<T> withBacklogSize(SizeT backlogSize) {
            this.backlogSize = backlogSize;
            return this;
        }

        ServerConfig<T> build() {
            record serverConfig<R extends InetAddress>(SocketAddress<R> address, Set<SocketFlag> listenerFlags,
                                                       Set<SocketFlag> acceptorFlags, Set<SocketOption> listenerOptions, SizeT backlogSize)
                implements ServerConfig<R> {}

            return new serverConfig<>(SocketAddress.genericAddress(port, address), listenerFlags, acceptorFlags,
                                      listenerOptions, backlogSize);
        }
    }
}

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

package org.pragmatica.io.net.tcp;

import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.net.*;

import java.util.Set;

public interface ListenConfig<T extends InetAddress> {
    SocketAddress<T> address();

    Set<SocketFlag> listenerFlags();

    Set<SocketFlag> acceptorFlags();

    Set<SocketOption> listenerOptions();

    SizeT backlogSize();

    static ListenConfigBuilder<InetAddress.Inet4Address> listenConfig() {
        return listenConfig(InetAddress.Inet4Address.INADDR_ANY);
    }

    static ListenConfigBuilder<InetAddress.Inet4Address> listenConfig(InetAddress.Inet4Address address) {
        return new ListenConfigBuilder<>(address);
    }

    static ListenConfigBuilder<InetAddress.Inet6Address> listenConfig6() {
        return listenConfig6(InetAddress.Inet6Address.INADDR_ANY);
    }

    static ListenConfigBuilder<InetAddress.Inet6Address> listenConfig6(InetAddress.Inet6Address address) {
        return new ListenConfigBuilder<>(address);
    }

    class ListenConfigBuilder<T extends InetAddress> {
        private InetPort port = InetPort.inetPort(8081);
        private Set<SocketFlag> listenerFlags = SocketFlag.closeOnExec();
        private Set<SocketFlag> acceptorFlags = SocketFlag.closeOnExec();
        private Set<SocketOption> listenerOptions = SocketOption.reuseAll();
        private SizeT backlogSize = SizeT.sizeT(16);
        private final T address;

        private ListenConfigBuilder(T address) {
            this.address = address;
        }

        public ListenConfigBuilder<T> withPort(int port) {
            return withPort(InetPort.inetPort(port));
        }

        public ListenConfigBuilder<T> withPort(InetPort port) {
            this.port = port;
            return this;
        }

        public ListenConfigBuilder<T> withListenerFlags(Set<SocketFlag> listenerFlags) {
            this.listenerFlags = listenerFlags;
            return this;
        }

        public ListenConfigBuilder<T> withAcceptorFlags(Set<SocketFlag> acceptorFlags) {
            this.acceptorFlags = acceptorFlags;
            return this;
        }

        public ListenConfigBuilder<T> withListenerOptions(Set<SocketOption> listenerOptions) {
            this.listenerOptions = listenerOptions;
            return this;
        }

        public ListenConfigBuilder<T> withBacklogSize(SizeT backlogSize) {
            this.backlogSize = backlogSize;
            return this;
        }

        public ListenConfig<T> build() {
            record listenConfig<R extends InetAddress>(SocketAddress<R> address, Set<SocketFlag> listenerFlags,
                                                       Set<SocketFlag> acceptorFlags, Set<SocketOption> listenerOptions, SizeT backlogSize)
                implements ListenConfig<R> {}

            return new listenConfig<>(SocketAddress.genericAddress(port, address), listenerFlags, acceptorFlags,
                                      listenerOptions, backlogSize);
        }
    }
}

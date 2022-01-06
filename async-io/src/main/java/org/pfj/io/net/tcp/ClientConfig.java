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

import org.pfj.io.async.Timeout;
import org.pfj.io.async.net.InetAddress;
import org.pfj.io.async.net.InetPort;
import org.pfj.io.async.net.SocketAddress;
import org.pfj.lang.Option;

public interface ClientConfig<T extends InetAddress> {
    SocketAddress<T> address();

    Option<Timeout> connectTimeout();

    static <T extends InetAddress> ClientConfigBuilder<T> config(int port, T host) {
        return new ClientConfigBuilder<>(port, host);
    }

    class ClientConfigBuilder<T extends InetAddress> {
        private final int port;
        private final T host;
        private Option<Timeout> timeout = Option.empty();

        private ClientConfigBuilder(int port, T host) {
            this.port = port;
            this.host = host;
        }

        ClientConfigBuilder<T> withTimeout(Timeout timeout) {
            this.timeout = Option.option(timeout);
            return this;
        }

        ClientConfig<T> build() {
            record clientConfig<R extends InetAddress>(SocketAddress<R> address, Option<Timeout> connectTimeout)
            implements ClientConfig<R> {}

            return new clientConfig<>(SocketAddress.genericAddress(InetPort.inetPort(port), host), timeout);
        }
    }
}

/*
 * Copyright (c) 2020 Sergiy Yevtushenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pfj.io.async.net;

import org.pfj.io.async.file.FileDescriptor;

/**
 * Connection context holds base client information - address and file descriptor.
 */
public interface ConnectionContext<T extends InetAddress> {
    FileDescriptor socket();

    SocketAddress<T> address();

    static <T extends InetAddress> ConnectionContext<T> connection(FileDescriptor socket, SocketAddress<T> address) {
        record connectionContext<K extends InetAddress>(FileDescriptor socket, SocketAddress<K> address)
            implements ConnectionContext<K> {}

        return new connectionContext<>(socket, address);
    }

    static ConnectionContext<Inet4Address> connectionIn(int fd, SocketAddressIn addressIn) {
        return connection(FileDescriptor.socket(fd), addressIn);
    }

    static ConnectionContext<Inet6Address> connectionIn6(int fd, SocketAddressIn6 addressIn6) {
        return connection(FileDescriptor.socket(fd), addressIn6);
    }
}

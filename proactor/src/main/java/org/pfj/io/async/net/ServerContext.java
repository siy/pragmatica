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
 * Server connector handles incoming external connections.
 */
public class ServerContext<T extends SocketAddress<?>> {
    private final FileDescriptor socket;
    private final T address;
    private final int queueLen;

    private ServerContext(FileDescriptor socket, T address, int queueLen) {
        this.socket = socket;
        this.address = address;
        this.queueLen = queueLen;
    }

    public static <T extends SocketAddress<?>> ServerContext<T> connector(FileDescriptor socket, SocketAddress<?> address, int queueLen) {
        return new ServerContext<>(socket, (T) address, queueLen);
    }

    public FileDescriptor socket() {
        return socket;
    }

    public T address() {
        return address;
    }

    public int queueLen() {
        return queueLen;
    }

    @Override
    public String toString() {
        return "ServerContext(" + socket + ", " + address + ')';
    }
}

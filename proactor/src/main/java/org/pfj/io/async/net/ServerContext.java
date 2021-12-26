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

import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Server connector handles incoming external connections.
 */
public class ServerContext<T extends SocketAddress<?>> {
    private final FileDescriptor socket;
    private final T address;
    private final int queueDepth;
    //private final ConcurrentMap<ULID, IncomingConnectionContext> connections = new ConcurrentHashMap<>();
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    private ServerContext(FileDescriptor socket, T address, int queueDepth) {
        this.socket = socket;
        this.address = address;
        this.queueDepth = queueDepth;
    }

    public static <T extends SocketAddress<?>> ServerContext<T> connector(FileDescriptor socket, SocketAddress<?> address, int queueDepth) {
        return new ServerContext<>(socket, (T) address, queueDepth);
    }

    public FileDescriptor socket() {
        return socket;
    }

    public T address() {
        return address;
    }

    public int queueDepth() {
        return queueDepth;
    }

//    public ServerContext<T> addConnection( IncomingConnectionContext incomingConnectionContext) {
//        connections.putIfAbsent(incomingConnectionContext.id(), incomingConnectionContext);
//        return this;
//    }
//
//    public ServerContext<T> removeConnection( IncomingConnectionContext incomingConnectionContext) {
//        connections.remove(incomingConnectionContext.id());
//        return this;
//    }

    @Override
    public String toString() {
        return "ServerContext(" + socket + ", " + address + ')';
    }

    public boolean shutdownInProgress() {
        return shutdown.get();
    }

    //TODO: does not look convenient nor good enough, how to rework it?
//    public void shutdown( Promise<Unit> shutdownPromise) {
//        if (shutdown.compareAndSet(false, true)) {
//            //wait for all connections to be closed???
//            shutdownPromise.ok(unit());
//        }
//    }
}

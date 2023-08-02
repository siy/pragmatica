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

import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.net.*;
import org.pragmatica.io.async.net.InetAddress.Inet4Address;
import org.pragmatica.io.async.net.InetAddress.Inet6Address;
import org.pragmatica.io.net.AcceptProtocol;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * TCP/IP Listener configuration.
 */
public interface ListenConfig<T extends InetAddress> {
    int DEFAULT_PORT = 8081;
    int DEFAULT_BACKLOG_SIZE = 16;

    SocketAddress<T> address();

    /**
     * Flags for {@link Proactor#listen(Consumer, SocketAddress, SocketType, Set, SizeT, Set)} API call.
     */
    Set<SocketFlag> listenerFlags();

    /**
     * Flags for {@link Proactor#accept(BiConsumer, FileDescriptor, Set, InetAddress)} API call.
     */
    Set<SocketFlag> acceptorFlags();

    /**
     * Socket options for {@link Proactor#listen(Consumer, SocketAddress, SocketType, Set, SizeT, Set)} API call.
     */
    Set<SocketOption> listenerOptions();

    /**
     * Length of the backlog queue of the incoming connections.
     */
    SizeT backlogSize();

    /**
     * The handler for the accepted connections.
     */
    AcceptProtocol<T> acceptProtocol();

    /**
     * Create configuration builder to listen on all available interfaces using provided handler for accepted connections.
     * <p>
     * This version supports TCP/IPv4 connections.
     *
     * @param acceptProtocol The accepted connection protocol (handler)
     *
     * @return Configuration builder
     */
    static ListenConfigBuilder<Inet4Address> listenConfig(AcceptProtocol<Inet4Address> acceptProtocol) {
        return listenConfig(Inet4Address.INADDR_ANY, acceptProtocol);
    }

    /**
     * Create configuration builder to listen on specified IP address (interface) using provided handler for accepted connections.
     * <p>
     * This version supports TCP/IPv4 connections.
     *
     * @param address        The address to listen on
     * @param acceptProtocol The accepted connection protocol (handler)
     *
     * @return Configuration builder
     */
    static ListenConfigBuilder<Inet4Address> listenConfig(Inet4Address address, AcceptProtocol<Inet4Address> acceptProtocol) {
        return new ListenConfigBuilder<>(address, acceptProtocol);
    }

    /**
     * Create configuration builder to listen on all available interfaces using provided handler for accepted connections.
     * <p>
     * This version supports TCP/IPv6 connections.
     *
     * @param acceptProtocol The accepted connection protocol (handler)
     *
     * @return Configuration builder
     */
    static ListenConfigBuilder<Inet6Address> listenConfig6(AcceptProtocol<Inet6Address> acceptProtocol) {
        return listenConfig6(Inet6Address.INADDR_ANY, acceptProtocol);
    }

    /**
     * Create configuration builder to listen on specified IP address (interface) using provided handler for accepted connections.
     * <p>
     * This version supports TCP/IPv4 connections.
     *
     * @param address        The address to listen on
     * @param acceptProtocol The accepted connection protocol (handler)
     *
     * @return Configuration builder
     */
    static ListenConfigBuilder<Inet6Address> listenConfig6(Inet6Address address, AcceptProtocol<Inet6Address> acceptProtocol) {
        return new ListenConfigBuilder<>(address, acceptProtocol);
    }

    class ListenConfigBuilder<T extends InetAddress> {
        private InetPort port = InetPort.inetPort(DEFAULT_PORT);
        private Set<SocketFlag> listenerFlags = SocketFlag.closeOnExec();
        private Set<SocketFlag> acceptorFlags = SocketFlag.closeOnExec();
        private Set<SocketOption> listenerOptions = SocketOption.reuseAll();
        private SizeT backlogSize = SizeT.sizeT(DEFAULT_BACKLOG_SIZE);
        private final AcceptProtocol<T> acceptProtocol;
        private final T address;

        private ListenConfigBuilder(T address, AcceptProtocol<T> acceptProtocol) {
            this.address = address;
            this.acceptProtocol = acceptProtocol;
        }

        /**
         * Configure listen port.
         *
         * @param port The port number
         *
         * @return Builder instance for fluent call chaining
         */
        public ListenConfigBuilder<T> withPort(int port) {
            return withPort(InetPort.inetPort(port));
        }

        /**
         * Configure listen port.
         *
         * @param port The port number
         *
         * @return Builder instance for fluent call chaining
         */
        public ListenConfigBuilder<T> withPort(InetPort port) {
            this.port = port;
            return this;
        }

        /**
         * Configure listener flags.
         *
         * @param listenerFlags The listener flags. See {@link SocketFlag} for more details
         *
         * @return Builder instance for fluent call chaining
         */
        public ListenConfigBuilder<T> withListenerFlags(Set<SocketFlag> listenerFlags) {
            this.listenerFlags = listenerFlags;
            return this;
        }

        /**
         * Configure acceptor flags.
         *
         * @param acceptorFlags The acceptor flags. See {@link SocketFlag} for more details
         *
         * @return Builder instance for fluent call chaining
         */
        public ListenConfigBuilder<T> withAcceptorFlags(Set<SocketFlag> acceptorFlags) {
            this.acceptorFlags = acceptorFlags;
            return this;
        }

        /**
         * Configure listener options
         *
         * @param listenerOptions The listener options. See {@link SocketOption} for more details
         *
         * @return Builder instance for fluent call chaining
         */
        public ListenConfigBuilder<T> withListenerOptions(Set<SocketOption> listenerOptions) {
            this.listenerOptions = listenerOptions;
            return this;
        }

        /**
         * Configure backlog size.
         *
         * @param backlogSize Connection backlog queue size
         *
         * @return Builder instance for fluent call chaining
         */
        public ListenConfigBuilder<T> withBacklogSize(SizeT backlogSize) {
            this.backlogSize = backlogSize;
            return this;
        }

        /**
         * Build configuration instance.
         *
         * @return Built instance
         */
        public ListenConfig<T> build() {
            record listenConfig<R extends InetAddress>(SocketAddress<R> address, Set<SocketFlag> listenerFlags,
                                                       Set<SocketFlag> acceptorFlags, Set<SocketOption> listenerOptions,
                                                       SizeT backlogSize, AcceptProtocol<R> acceptProtocol)
                implements ListenConfig<R> {}

            return new listenConfig<>(SocketAddress.genericAddress(port, address), listenerFlags, acceptorFlags,
                                      listenerOptions, backlogSize, acceptProtocol);
        }
    }
}

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
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.net.*;
import org.pragmatica.io.net.Client;
import org.pragmatica.io.net.ClientProtocol;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import static org.pragmatica.io.async.net.ConnectionContext.connection;

/**
 * Simple TCP/IP client.
 */
//TODO: tests
public class TcpClient<T extends InetAddress> implements Client {
    private final ClientConfig<T> config;

    private TcpClient(ClientConfig<T> config) {
        this.config = config;
    }

    /**
     * Create client instance with provided configuration.
     *
     * @param config Client configuration
     *
     * @return Created client
     */
    public static <T extends InetAddress> Client tcpClient(ClientConfig<T> config) {
        return new TcpClient<>(config);
    }

    /**
     * Run client with the provided client protocol, see {@link ClientProtocol} for more details.
     *
     * @param protocol Client protocol (handler)
     *
     * @return Promise, which will be resolved with the result of the processing of request.
     */
    @Override
    public <R> Promise<R> access(ClientProtocol<R> protocol) {
        return Promise.promise((promise, proactor) -> createSocket(promise, proactor, protocol));
    }

    private <R> void createSocket(Promise<R> promise, Proactor proactor, ClientProtocol<R> protocol) {
        proactor.socket((result, proactor1) -> result.onFailure(promise::failure)
                                                     .onSuccess(socket -> doConnect(socket, promise, proactor1, protocol))
                                                     .onSuccess(socket -> closeSocket(promise, socket)),
                        AddressFamily.INET, SocketType.STREAM, SocketFlag.none(), SocketOption.reuseAll());
    }

    private <R> void closeSocket(Promise<R> promise, FileDescriptor socket) {
        promise.onResultDo(() -> promise.asyncIO(proactor -> proactor.close(Unit::unit, socket)));
    }

    private <R> void doConnect(FileDescriptor socket, Promise<R> promise, Proactor proactor, ClientProtocol<R> protocol) {
        proactor.connect((result, proactor1) -> handleConnection(result, protocol, proactor1, socket, promise),
                         socket, config.address(), config.connectTimeout());
    }

    private <R> void handleConnection(Result<FileDescriptor> result,
                                      ClientProtocol<R> protocol,
                                      Proactor proactor1,
                                      FileDescriptor socket,
                                      Promise<R> promise) {
        result.onFailure(promise::failure)
              .onSuccessDo(() -> protocol.process(connection(socket, config.address()), proactor1)
                                         .onResult(promise::resolve));
    }

}

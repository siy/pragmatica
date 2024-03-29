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

import org.pragmatica.io.async.SystemError;
import org.pragmatica.io.async.net.ConnectionContext;
import org.pragmatica.io.async.net.InetAddress;
import org.pragmatica.io.async.net.ListenContext;
import org.pragmatica.io.async.net.SocketType;
import org.pragmatica.io.net.Listener;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

import static org.pragmatica.io.async.Proactor.proactor;
import static org.pragmatica.io.async.util.DaemonThreadFactory.shutdownThreadFactory;
import static org.pragmatica.io.net.ConnectionProtocolContext.connectionProtocolContext;
import static org.pragmatica.lang.Promise.runAsync;

/**
 * TCP/IP incoming connection listener (server).
 */
public class TcpListener<T extends InetAddress> implements Listener<T> {
    private static final Logger LOG = LoggerFactory.getLogger(TcpListener.class);

    private final ListenConfig<T> config;
    private final Promise<Unit> shutdown = Promise.promise();
    private final Promise<Unit> serve = Promise.promise();
    private final AtomicReference<ListenContext<T>> serverContext = new AtomicReference<>();

    private TcpListener(ListenConfig<T> config) {
        this.config = config;
    }

    /**
     * Create listener instance using provided configuration.
     *
     * @param config Listener configuration.
     *
     * @return Created listener
     */
    public static <T extends InetAddress> Listener<T> tcpListener(ListenConfig<T> config) {
        return new TcpListener<>(config);
    }

    /**
     * Start listening for incoming connection.
     *
     * @return Promise which will be resolved when server will stop listening for incoming connection (due to failure or shutdown)
     */
    @Override
    public Promise<Unit> listen() {
        Runtime.getRuntime()
               .addShutdownHook(shutdownThreadFactory().newThread(this::shutdown));

        runAsync(() -> proactor().listen(this::doListen,
                                                 config.address(), SocketType.STREAM, config.listenerFlags(),
                                                 config.backlogSize(), config.listenerOptions()));
        return serve;
    }

    /**
     * Initiate server shutdown.
     *
     * @return Promise which will be resolved when shutdown will be finished.
     */
    @Override
    public Promise<Unit> shutdown() {
        if (serverContext.get() != null) {
            proactor().close(shutdown::resolve, serverContext.get().socket(), Option.empty());
        }

        return shutdown;
    }

    private void doListen(Result<ListenContext<T>> result) {
        result.onFailure(serve::failure)
              .onFailure(shutdown::failure)
              .onSuccess(context -> LOG.debug("Listening at {}", context))
              .onSuccess(this::doAccept);
    }

    private void doAccept(ListenContext<T> context) {
        if (!serverContext.compareAndSet(null, context)) {
            serve.failure(SystemError.EALREADY);
            return;
        }

        runAsync(() -> repeatAccept(context));
    }

    private void repeatAccept(ListenContext<T> context) {
        proactor().accept((result, proactor1) -> processAccept(context, result), context.socket(),
                          config.acceptorFlags(), context.address().address());
    }

    private void processAccept(ListenContext<T> context, Result<ConnectionContext<T>> result) {
        result.onFailure(failure -> LOG.warn("Accept error: {}", failure.message()))
              .onFailure(serve::failure)
              .onSuccess(connectionContext -> handleSuccessfulAccept(context, connectionContext));
    }

    private void handleSuccessfulAccept(ListenContext<T> context,
                                        ConnectionContext<T> connectionContext) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Accepted connection {}", connectionContext);
        }

        runAsync(() -> config.acceptProtocol().accept(connectionProtocolContext(context, connectionContext)));
        repeatAccept(context);
    }
}

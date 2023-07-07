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
import org.pragmatica.io.async.SystemError;
import org.pragmatica.io.async.net.ConnectionContext;
import org.pragmatica.io.async.net.InetAddress;
import org.pragmatica.io.async.net.ListenContext;
import org.pragmatica.io.async.net.SocketType;
import org.pragmatica.io.async.util.DaemonThreadFactory;
import org.pragmatica.io.net.Listener;
import org.pragmatica.io.net.AcceptProtocol;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.task.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

public class TcpListener<T extends InetAddress> implements Listener<T> {
    private static final Logger LOG = LoggerFactory.getLogger(TcpListener.class);
    private static final ThreadFactory SHUTDOWN_HOOK_THREAD_FACTORY = DaemonThreadFactory.threadFactory("Shutdown Hook");

    private final ListenConfig<T> config;
    private final Promise<Unit> shutdown = Promise.promise();
    private final Promise<Unit> serve = Promise.promise();
    private final AtomicReference<ListenContext<T>> serverContext = new AtomicReference<>();

    private TcpListener(ListenConfig<T> config) {
        this.config = config;
    }

    public static <T extends InetAddress> Listener<T> tcpListener(ListenConfig<T> config) {
        return new TcpListener<>(config);
    }

    @Override
    public Promise<Unit> listen(AcceptProtocol<T> protocol) {
        return serve.async((promise, proactor) ->
                               proactor.listen(result -> doListen(result, protocol),
                                               config.address(), SocketType.STREAM, config.listenerFlags(),
                                               config.backlogSize(), config.listenerOptions()));
    }

    @Override
    public Promise<Unit> shutdown() {
        if (serverContext.get() != null) {
            shutdown.async((promise, proactor) -> proactor.close(promise::resolve, serverContext.get().socket(), Option.empty()));
        }

        return shutdown;
    }

    @Override
    public Thread shutdownHook() {
        return SHUTDOWN_HOOK_THREAD_FACTORY.newThread(this::shutdown);
    }

    private void doListen(Result<ListenContext<T>> result, AcceptProtocol<T> protocol) {
        result.onFailure(serve::failure)
              .onFailure(shutdown::failure)
              .onSuccess(context -> LOG.debug("Listening at {}", context))
              .onSuccess(context -> doAccept(context, protocol));
    }

    private void doAccept(ListenContext<T> context, AcceptProtocol<T> protocol) {
        if (!serverContext.compareAndSet(null, context)) {
            serve.failure(SystemError.EALREADY);
            return;
        }

        serve.async((promise, proactor, executor) -> repeatAccept(proactor, context, protocol, executor));
    }

    private void repeatAccept(Proactor proactor, ListenContext<T> context, AcceptProtocol<T> protocol, TaskExecutor executor) {
        proactor.accept((result, proactor1) -> processAccept(context, protocol, result, proactor1, executor), context.socket(),
                        config.acceptorFlags(), context.address().address());
    }

    private void processAccept(ListenContext<T> context, AcceptProtocol<T> protocol, Result<ConnectionContext<T>> result,
                               Proactor proactor, TaskExecutor executor) {
        result.onFailure(failure -> LOG.warn("Accept error: {}", failure.message()))
              .onFailure(serve::failure)
              //.onSuccess(connectionContext -> LOG.info("Accepted connection {} at {}", connectionContext, Thread.currentThread().getName()))
              .onSuccess(connectionContext -> executor.submit(proactor1 -> protocol.accept(context, connectionContext, proactor1)))
              .onSuccess(__ -> repeatAccept(proactor, context, protocol, executor));
    }
}

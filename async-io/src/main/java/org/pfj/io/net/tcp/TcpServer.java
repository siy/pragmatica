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

import org.pfj.io.async.Proactor;
import org.pfj.io.async.SystemError;
import org.pfj.io.async.net.*;
import org.pfj.io.net.ServerProtocol;
import org.pfj.io.net.Server;
import org.pfj.lang.Option;
import org.pfj.lang.Promise;
import org.pfj.lang.Result;
import org.pfj.lang.Unit;
import org.pfj.task.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static java.util.stream.IntStream.range;

public class TcpServer<T extends InetAddress> implements Server<T> {
    private static final Logger LOG = LoggerFactory.getLogger(TcpServer.class);

    private final ServerConfig<T> config;
    private final Promise<Unit> shutdown = Promise.promise();
    private final AtomicReference<ServerContext<?>> serverContext = new AtomicReference<>();

    private TcpServer(ServerConfig<T> config) {
        this.config = config;
    }

    public static <T extends InetAddress> Server<T> tcpServer(ServerConfig<T> config) {
        return new TcpServer<>(config);
    }

    @Override
    public Promise<Unit> serve(ServerProtocol protocol) {
        return start(Promise.promise(), protocol);
    }

    @Override
    public Promise<Unit> shutdown() {
        if (serverContext.get() != null) {
            shutdown.async((promise, proactor) -> proactor.close(promise::resolve, serverContext.get().socket(), Option.empty()));
        }

        return shutdown;
    }

    private Promise<Unit> start(Promise<Unit> servePromise, ServerProtocol protocol) {
        return servePromise.async((promise, proactor) ->
                                      proactor.server(result -> startServing(result, servePromise, protocol),
                                                      config.address(), SocketType.STREAM, config.listenerFlags(),
                                                      config.backlogSize(), config.listenerOptions()));
    }

    private void startServing(Result<ServerContext<T>> result, Promise<Unit> servePromise, ServerProtocol protocol) {
        result.onFailure(servePromise::failure)
              .onFailure(shutdown::failure)
              .onSuccess(context -> startAccepting(servePromise, context, protocol));
    }

    private void startAccepting(Promise<Unit> servePromise, ServerContext<T> context, ServerProtocol protocol) {
        if (!serverContext.compareAndSet(null, context)) {
            servePromise.failure(SystemError.EALREADY);
            return;
        }

        servePromise.async((promise, proactor, executor) -> executor.submit(makeTasks(executor, context, protocol))
                                                                    .submit(__ -> promise.success(Unit.unit())));
    }

    private List<Consumer<Proactor>> makeTasks(TaskExecutor executor, ServerContext<T> context, ServerProtocol protocol) {
        return range(0, executor.parallelism())
            .mapToObj(__ -> makeHandler(context, protocol))
            .toList();
    }

    private Consumer<Proactor> makeHandler(ServerContext<T> context, ServerProtocol protocol) {
        return (proactor) -> initAccept(proactor, context, protocol);
    }

    private void initAccept(Proactor proactor, ServerContext<T> context, ServerProtocol protocol) {
        proactor.accept((result, proactor1) -> processAccept(context, protocol, result, proactor1), context.socket(),
                        config.acceptorFlags(), context.address().address());
    }

    private void processAccept(ServerContext<T> context, ServerProtocol protocol, Result<ConnectionContext<T>> result, Proactor proactor) {
        result.onFailure(failure -> LOG.info("Accept error: {}", failure.message()))
              .onSuccess(connectionContext -> protocol.start(context, connectionContext, proactor))
              .onSuccess(__ -> initAccept(proactor, context, protocol));
    }
}

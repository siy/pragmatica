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

package org.pragmatica.io.net.protocols;

import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.Timeout;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.net.InetAddress;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.io.net.AcceptProtocol;
import org.pragmatica.io.net.ConnectionProtocol;
import org.pragmatica.io.net.ConnectionProtocolContext;
import org.pragmatica.lang.Functions.FN1;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

import static org.pragmatica.io.async.Proactor.proactor;

/**
 * Simple TCP Echo protocol implementation. It just sends back received data.
 */
public sealed interface EchoProtocol<T extends InetAddress> extends ConnectionProtocol<T> {

    static <T extends InetAddress> AcceptProtocol<T> acceptEchoProtocol(int bufferSize, Option<Timeout> timeout) {
        return context -> new EchoProtocolImpl<>(new EchoProtocolConfig(bufferSize, timeout), context).process();
    }

    record EchoProtocolConfig(int bufferSize, Option<Timeout> timeout) {}

    final class EchoProtocolImpl<T extends InetAddress> implements EchoProtocol<T> {
        private static final Logger LOG = LoggerFactory.getLogger(EchoProtocol.class);

        private final ReadHandler readHandler;

        public EchoProtocolImpl(EchoProtocolConfig config, ConnectionProtocolContext<T> context) {
            FileDescriptor socket = context.connectionContext().socket();
            OffHeapSlice buffer = OffHeapSlice.fixedSize(config.bufferSize());
            FailureHandler failureHandler = new FailureHandler(socket);
            this.readHandler = new ReadHandler(socket, buffer, config.timeout(), failureHandler);
            readHandler.writeHandler = new WriteHandler(socket, buffer, config.timeout(), failureHandler, readHandler);
        }

        @Override
        public void process() {
            readHandler.proactor = proactor();
            readHandler.apply(SizeT.ZERO);
        }

        static class FailureHandler implements FN1<Unit, Result.Cause> {
            private final FileDescriptor socket;

            Proactor proactor;

            FailureHandler(FileDescriptor socket) {
                this.socket = socket;
            }

            @Override
            public Unit apply(Result.Cause failure) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("I/O error: {}", failure);

                    Proactor.stats().forEach(s -> Proactor.stats().forEach(System.err::println));
                }

                proactor.close(this::logClosing, socket);

                return Unit.unit();
            }

            private void logClosing(Result<Unit> unused) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Socket {} closed", socket);
                }
            }
        }

        static class ReadHandler implements BiConsumer<Result<SizeT>, Proactor>, FN1<Unit, SizeT> {
            private final FileDescriptor socket;
            private final OffHeapSlice buffer;
            private final Option<Timeout> timeout;
            private final FailureHandler failureHandler;
            private Proactor proactor;
            WriteHandler writeHandler;

            ReadHandler(FileDescriptor socket, OffHeapSlice buffer, Option<Timeout> timeout, FailureHandler failureHandler) {
                this.socket = socket;
                this.buffer = buffer;
                this.timeout = timeout;
                this.failureHandler = failureHandler;
            }

            @Override
            public void accept(Result<SizeT> result, Proactor proactor) {
                this.proactor = proactor;
                result.fold(failureHandler, this);
            }

            @Override
            public Unit apply(SizeT size) {
                proactor.read(writeHandler, socket, buffer, timeout);
                return Unit.unit();
            }
        }

        static class WriteHandler implements BiConsumer<Result<SizeT>, Proactor>, FN1<Unit, SizeT> {
            private final FileDescriptor socket;
            private final OffHeapSlice buffer;
            private final Option<Timeout> timeout;
            private final FailureHandler failureHandler;
            private final ReadHandler readHandler;
            private Proactor proactor;

            WriteHandler(FileDescriptor socket, OffHeapSlice buffer, Option<Timeout> timeout, FailureHandler failureHandler, ReadHandler readHandler) {
                this.socket = socket;
                this.buffer = buffer;
                this.timeout = timeout;
                this.failureHandler = failureHandler;
                this.readHandler = readHandler;
            }

            @Override
            public void accept(Result<SizeT> result, Proactor proactor) {
                this.proactor = proactor;
                result.fold(failureHandler, this);
            }

            @Override
            public Unit apply(SizeT size) {
                proactor.write(readHandler, socket, buffer, timeout);
                return Unit.unit();
            }
        }
    }
}

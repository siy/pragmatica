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

import org.pragmatica.io.async.Timeout;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.net.InetAddress;
import org.pragmatica.io.async.util.allocator.FixedBuffer;
import org.pragmatica.io.net.AcceptProtocol;
import org.pragmatica.io.net.ConnectionProtocol;
import org.pragmatica.io.net.ConnectionProtocolContext;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Result.Cause;
import org.pragmatica.lang.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.pragmatica.io.async.Proactor.proactor;
import static org.pragmatica.lang.Option.option;

/**
 * Simple TCP Echo protocol implementation. All it does is sending back received data.
 * <p>
 * This version uses fixed buffers to transfer data between application and OS.
 */
public sealed interface FixedBuffersEchoProtocol<T extends InetAddress> extends ConnectionProtocol<T> {
    static <T extends InetAddress> AcceptProtocol<T> acceptEchoProtocol(int bufferSize, Option<Timeout> timeout) {
        return context -> new EchoProtocolImpl<>(new EchoProtocolConfig<>(bufferSize, timeout), context).process();
    }

    record EchoProtocolConfig<T extends InetAddress>(int bufferSize, Option<Timeout> timeout) {}

    final class EchoProtocolImpl<T extends InetAddress> implements FixedBuffersEchoProtocol<T> {
        private static final Logger LOG = LoggerFactory.getLogger(FixedBuffersEchoProtocol.class);

        private final EchoProtocolConfig<T> config;
        private final FileDescriptor socket;
        private FixedBuffer buffer;

        public EchoProtocolImpl(EchoProtocolConfig<T> config, ConnectionProtocolContext<T> context) {
            this.config = config;
            this.socket = context.connectionContext().socket();
        }

        @Override
        public void process() {
            proactor().allocateFixedBuffer(config.bufferSize())
                      .onFailure(this::handleFailure)
                      .onSuccess(buf -> buffer = buf);

            proactor().readFixed(this::readHandler, socket, buffer, config.timeout());
        }

        private void readHandler(Result<SizeT> result) {
            result.fold(this::handleFailure, size -> {
                proactor().writeFixed(this::writeHandler, socket, buffer, config.timeout());
                return Unit.unit();
            });
        }

        private void writeHandler(Result<SizeT> result) {
            result.fold(this::handleFailure, size -> {
                proactor().readFixed(this::readHandler, socket, buffer, config.timeout());
                return Unit.unit();
            });
        }

        private Unit handleFailure(Cause failure) {
            if (LOG.isInfoEnabled()) {
                LOG.info("I/O error: {}", failure);
            }

            option(buffer).onPresent(FixedBuffer::dispose);
            proactor().close(this::logClosing, socket);

            return Unit.unit();
        }

        private void logClosing(Result<Unit> unused) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Socket {} closed", socket);
            }
        }
    }
}

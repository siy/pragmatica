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
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple TCP Echo protocol implementation. It just sends back received data.
 */
public sealed interface EchoProtocol<T extends InetAddress> extends ConnectionProtocol<T> {

    static <T extends InetAddress> AcceptProtocol<T> acceptEchoProtocol(T addressTag, int bufferSize, Option<Timeout> timeout) {
        var config = new EchoProtocolConfig<T>(bufferSize, timeout);
        return (context, proactor) -> new  EchoProtocolImpl<T>(config, context).process(proactor);
    }

    record EchoProtocolConfig<T extends InetAddress>(int bufferSize, Option<Timeout> timeout) {}

    final class EchoProtocolImpl<T extends InetAddress> implements EchoProtocol<T> {
        private static final Logger LOG = LoggerFactory.getLogger(EchoProtocol.class);

        private final EchoProtocolConfig<T> config;
        private final OffHeapSlice buffer;
        private final FileDescriptor socket;

        public EchoProtocolImpl(EchoProtocolConfig<T> config, ConnectionProtocolContext<T> context) {
            this.config = config;
            this.socket = context.connectionContext().socket();
            this.buffer = OffHeapSlice.fixedSize(config.bufferSize());
        }

        @Override
        public void process(Proactor proactor) {
            proactor.read(this::readHandler, socket, buffer, config.timeout());
        }

        private void readHandler(Result<SizeT> result, Proactor proactor) {
            result.fold(failure -> handleFailure(failure, proactor), size -> {
                proactor.write(this::writeHandler, socket, buffer, config.timeout());
                return Unit.unit();
            });
        }

        private void writeHandler(Result<SizeT> result, Proactor proactor) {
            result.fold(failure -> handleFailure(failure, proactor), size -> {
                proactor.read(this::readHandler, socket, buffer, config.timeout());
                return Unit.unit();
            });
        }

        private Unit handleFailure(Result.Cause failure, Proactor proactor) {
            if (LOG.isInfoEnabled()) {
                LOG.info("I/O error: {}", failure);
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
}

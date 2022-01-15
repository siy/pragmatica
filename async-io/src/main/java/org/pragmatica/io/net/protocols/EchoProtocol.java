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
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.net.InetAddress;
import org.pragmatica.io.async.util.OffHeapBuffer;
import org.pragmatica.io.net.ConnectionProtocol;
import org.pragmatica.io.net.ConnectionProtocolContext;
import org.pragmatica.io.net.ConnectionProtocolStarter;
import org.pragmatica.lang.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple TCP Echo protocol implementation. All it does is sending back received data.
 */
public interface EchoProtocol<T extends InetAddress> extends ConnectionProtocol<T> {
    static <T extends InetAddress> ConnectionProtocolStarter<T> starter(int bufferSize, Option<Timeout> timeout) {
        return new EchoProtocolConfig<>(bufferSize, timeout);
    }

    record EchoProtocolConfig<T extends InetAddress>(int bufferSize, Option<Timeout> timeout) implements ConnectionProtocolStarter<T> {
        @Override
        public void start(ConnectionProtocolContext<T> context, Proactor proactor) {
            new EchoProtocolImpl<T>(this, context)
                .run(proactor);
        }
    }

    class EchoProtocolImpl<T extends InetAddress> implements ConnectionProtocol<T> {
        private static final Logger LOG = LoggerFactory.getLogger(EchoProtocol.class);

        private final EchoProtocolConfig<T> config;
        private final ConnectionProtocolContext<T> context;
        private final OffHeapBuffer buffer;

        public EchoProtocolImpl(EchoProtocolConfig<T> config, ConnectionProtocolContext<T> context) {
            this.config = config;
            this.context = context;
            this.buffer = OffHeapBuffer.fixedSize(config.bufferSize());
        }

        @Override
        public void run(Proactor proactor) {
            startRead(proactor);
        }

        private FileDescriptor socket() {
            return context.connectionContext().socket();
        }

        private Option<Timeout> timeout() {
            return config.timeout();
        }

        private void startRead(Proactor proactor) {
            proactor.read((result, proactor1) -> result.onFailure(failure -> LOG.warn("Read error: {}", failure.message()))
                                                       .onFailureDo(() -> cleanup(proactor1))
                                                       .onSuccess(size -> startWrite(proactor1)),
                          socket(), buffer, timeout());
        }

        private void startWrite(Proactor proactor) {
            proactor.write((result, proactor1) -> result.onFailure(failure -> LOG.warn("Write error: {}", failure.message()))
                                                        .onFailureDo(() -> cleanup(proactor1))
                                                        .onSuccess(size -> startRead(proactor1)),
                           socket(), buffer, timeout());
        }

        private void cleanup(Proactor proactor) {
            proactor.close(result -> LOG.debug("Socket {} closed", socket()), socket());
        }
    }
}
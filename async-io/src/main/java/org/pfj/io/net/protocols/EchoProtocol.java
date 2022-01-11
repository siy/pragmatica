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

package org.pfj.io.net.protocols;

import org.pfj.io.async.Proactor;
import org.pfj.io.async.Timeout;
import org.pfj.io.async.common.OffsetT;
import org.pfj.io.async.file.FileDescriptor;
import org.pfj.io.async.net.InetAddress;
import org.pfj.io.async.util.OffHeapBuffer;
import org.pfj.io.net.ConnectionProtocol;
import org.pfj.io.net.ConnectionProtocolContext;
import org.pfj.io.net.ConnectionProtocolStarter;
import org.pfj.lang.Option;

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
            proactor.read((result, proactor1) -> result.onFailureDo(() -> cleanup(proactor1))
                                                       .onSuccess(size -> startWrite(proactor1)),
                          socket(), buffer, OffsetT.ZERO, timeout());
        }

        private void startWrite(Proactor proactor) {
            proactor.write((result, proactor1) -> result.onFailureDo(() -> cleanup(proactor1))
                                                        .onSuccess(size -> startRead(proactor1)),
                           socket(), buffer, OffsetT.ZERO, timeout());
        }

        private void cleanup(Proactor proactor) {
            proactor.close(socket());
        }
    }
}

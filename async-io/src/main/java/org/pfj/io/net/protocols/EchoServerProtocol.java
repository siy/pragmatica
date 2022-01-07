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
import org.pfj.io.async.net.ConnectionContext;
import org.pfj.io.async.net.ServerContext;
import org.pfj.io.async.util.OffHeapBuffer;
import org.pfj.io.net.ServerProtocol;
import org.pfj.lang.Option;

import java.util.HexFormat;

import static org.pfj.lang.Option.empty;
import static org.pfj.lang.Option.option;

public class EchoServerProtocol implements ServerProtocol {
    private static final int DEFAULT_READ_BUFFER_SIZE = 16 * 1024;

    private final OffHeapBuffer buffer;
    private final Option<Timeout> timeout;

    private ServerContext<?> server;
    private ConnectionContext<?> connection;

    private EchoServerProtocol(Option<Timeout> timeout, int bufferSize) {
        this.timeout = timeout;
        this.buffer = OffHeapBuffer.fixedSize(bufferSize);
    }

    public static EchoServerProtocol echoServer() {
        return echoServer(empty(), DEFAULT_READ_BUFFER_SIZE);
    }

    public static EchoServerProtocol echoServer(Timeout timeout) {
        return echoServer(option(timeout), DEFAULT_READ_BUFFER_SIZE);
    }

    public static EchoServerProtocol echoServer(Timeout timeout, int bufferSize) {
        return echoServer(option(timeout), bufferSize);
    }

    public static EchoServerProtocol echoServer(int bufferSize) {
        return echoServer(empty(), bufferSize);
    }

    private static EchoServerProtocol echoServer(Option<Timeout> timeout, int bufferSize) {
        return new EchoServerProtocol(timeout, bufferSize);
    }

    @Override
    public void start(ServerContext<?> server, ConnectionContext<?> connection, Proactor proactor) {
        this.server = server;
        this.connection = connection;

        startRead(proactor);
    }

    private void startRead(Proactor proactor) {
        proactor.read((result, proactor1) -> result.onFailureDo(() -> cleanup(proactor1))
                                                   .onSuccess(size -> startWrite(proactor1)),
                      connection.socket(), buffer, OffsetT.ZERO, timeout);
    }

    private void startWrite(Proactor proactor) {
        proactor.write((result, proactor1) -> result.onFailureDo(() -> cleanup(proactor1))
                                                    .onSuccess(size -> startRead(proactor1)),
                       connection.socket(), buffer, OffsetT.ZERO, timeout);
    }

    private void cleanup(Proactor proactor) {
        proactor.close(connection.socket());
    }
}

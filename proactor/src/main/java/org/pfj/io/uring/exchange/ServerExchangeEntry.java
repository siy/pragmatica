/*
 * Copyright (c) 2020 Sergiy Yevtushenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pfj.io.uring.exchange;

import org.pfj.io.async.Submitter;
import org.pfj.io.async.common.SizeT;
import org.pfj.io.async.net.*;
import org.pfj.io.uring.UringHolder;
import org.pfj.io.uring.utils.PlainObjectPool;
import org.pfj.lang.Result;

import java.util.Set;
import java.util.function.BiConsumer;

import static org.pfj.io.uring.AsyncOperation.IORING_OP_NOP;

public class ServerExchangeEntry extends AbstractExchangeEntry<ServerExchangeEntry, ServerContext<?>> {
    private SocketAddress<?> socketAddress;
    private SocketType socketType;
    private Set<SocketFlag> openFlags;
    private SizeT queueDepth;
    private Set<SocketOption> options;

    protected ServerExchangeEntry(final PlainObjectPool<ServerExchangeEntry> pool) {
        super(IORING_OP_NOP, pool);
    }

    @Override
    protected void doAccept(final int result, final int flags, final Submitter submitter) {
        completion.accept(UringHolder.server(socketAddress,
                                             socketType,
                                             openFlags,
                                             options,
                                             queueDepth),
                          submitter);
    }

    public ServerExchangeEntry prepare(final BiConsumer<Result<ServerContext<?>>, Submitter> completion,
                                       final SocketAddress<?> socketAddress,
                                       final SocketType socketType,
                                       final Set<SocketFlag> openFlags,
                                       final SizeT queueDepth,
                                       final Set<SocketOption> options) {
        this.socketAddress = socketAddress;
        this.socketType = socketType;
        this.openFlags = openFlags;
        this.queueDepth = queueDepth;
        this.options = options;
        return super.prepare(completion);
    }
}

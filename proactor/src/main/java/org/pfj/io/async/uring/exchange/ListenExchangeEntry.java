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

package org.pfj.io.async.uring.exchange;

import org.pfj.io.async.Proactor;
import org.pfj.io.async.common.SizeT;
import org.pfj.io.async.net.*;
import org.pfj.io.async.uring.UringApi;
import org.pfj.io.async.uring.utils.PlainObjectPool;
import org.pfj.lang.Result;

import java.util.Set;
import java.util.function.BiConsumer;

import static org.pfj.io.async.uring.AsyncOperation.IORING_OP_NOP;

public class ListenExchangeEntry<T extends InetAddress> extends AbstractExchangeEntry<ListenExchangeEntry<T>, ListenContext<T>> {
    private SocketAddress<T> socketAddress;
    private SocketType socketType;
    private Set<SocketFlag> openFlags;
    private SizeT queueDepth;
    private Set<SocketOption> options;

    @SuppressWarnings("rawtypes")
    protected ListenExchangeEntry(PlainObjectPool<ListenExchangeEntry> pool) {
        super(IORING_OP_NOP, pool);
    }

    @Override
    protected void doAccept(int result, int flags, Proactor proactor) {
        completion.accept(UringApi.listen(socketAddress, socketType, openFlags, options, queueDepth), proactor);
    }

    public ListenExchangeEntry<T> prepare(BiConsumer<Result<ListenContext<T>>, Proactor> completion, SocketAddress<T> socketAddress,
                                          SocketType socketType, Set<SocketFlag> openFlags, SizeT queueDepth, Set<SocketOption> options) {
        this.socketAddress = socketAddress;
        this.socketType = socketType;
        this.openFlags = openFlags;
        this.queueDepth = queueDepth;
        this.options = options;
        return super.prepare(completion);
    }
}

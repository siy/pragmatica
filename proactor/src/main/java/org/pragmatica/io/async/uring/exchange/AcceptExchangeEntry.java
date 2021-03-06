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

package org.pragmatica.io.async.uring.exchange;

import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.SystemError;
import org.pragmatica.io.async.net.ConnectionContext;
import org.pragmatica.io.async.net.InetAddress;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapSocketAddress;
import org.pragmatica.io.async.uring.struct.raw.SQEntry;
import org.pragmatica.io.async.uring.utils.PlainObjectPool;
import org.pragmatica.lang.Result;

import java.util.function.BiConsumer;

import static org.pragmatica.io.async.net.ConnectionContext.connection;
import static org.pragmatica.io.async.uring.AsyncOperation.ACCEPT;

/**
 * Exchange entry for {@code accept} request.
 */
public class AcceptExchangeEntry<T extends InetAddress> extends AbstractExchangeEntry<AcceptExchangeEntry<T>, ConnectionContext<T>> {
    private final OffHeapSocketAddress clientAddress = OffHeapSocketAddress.v4();
    private int descriptor;
    private int acceptFlags;

    @SuppressWarnings("rawtypes")
    protected AcceptExchangeEntry(PlainObjectPool<AcceptExchangeEntry> pool) {
        super(ACCEPT, pool);
    }

    @Override
    public void close() {
        clientAddress.dispose();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doAccept(int res, int flags, Proactor proactor) {
        if (res <= 0) {
            completion.accept(SystemError.result(res), proactor);
        }

        completion.accept(clientAddress.extract().map(address -> (ConnectionContext<T>) connection(res, address)), proactor);
    }

    @Override
    public SQEntry apply(SQEntry entry) {
        return super.apply(entry)
                    .fd(descriptor)
                    .addr(clientAddress.sockAddrPtr())
                    .off(clientAddress.sizePtr())
                    .acceptFlags(acceptFlags);
    }

    public AcceptExchangeEntry<T> prepare(BiConsumer<Result<ConnectionContext<T>>, Proactor> completion, int descriptor, int acceptFlags, boolean v6) {
        this.descriptor = descriptor;
        this.acceptFlags = acceptFlags;
        clientAddress.protocolVersion(v6);
        return super.prepare(completion);
    }
}

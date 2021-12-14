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

import org.pfj.io.async.uring.struct.raw.RawSocketAddressIn;
import org.pfj.io.async.uring.struct.raw.SubmitQueueEntry;
import org.pfj.io.async.SystemError;
import org.pfj.io.async.Proactor;
import org.pfj.io.async.file.FileDescriptor;
import org.pfj.io.async.net.ClientConnection;
import org.pfj.io.async.net.SocketAddressIn;
import org.pfj.io.async.uring.struct.offheap.OffHeapSocketAddress;
import org.pfj.io.async.uring.utils.PlainObjectPool;
import org.pfj.lang.Result;

import java.util.function.BiConsumer;

import static org.pfj.io.async.net.ClientConnection.connectionIn;
import static org.pfj.io.async.uring.AsyncOperation.IORING_OP_ACCEPT;

//TODO: add support for v6
public class AcceptExchangeEntry extends AbstractExchangeEntry<AcceptExchangeEntry, ClientConnection<?>> {
    private final OffHeapSocketAddress<SocketAddressIn, RawSocketAddressIn> clientAddress = OffHeapSocketAddress.addressIn();
    private int descriptor;
    private int acceptFlags;

    protected AcceptExchangeEntry(final PlainObjectPool<AcceptExchangeEntry> pool) {
        super(IORING_OP_ACCEPT, pool);
    }

    @Override
    public void close() {
        clientAddress.dispose();
    }

    @Override
    protected void doAccept(final int res, final int flags, final Proactor proactor) {
        if (res <= 0) {
            completion.accept(SystemError.result(res), proactor);
        }

        completion.accept(clientAddress.extract()
                                       .map(addr -> connectionIn(FileDescriptor.socket(res), addr)),
            proactor);

    }

    @Override
    public SubmitQueueEntry apply(final SubmitQueueEntry entry) {
        return super.apply(entry)
                    .fd(descriptor)
                    .addr(clientAddress.sockAddrPtr())
                    .off(clientAddress.sizePtr())
                    .acceptFlags(acceptFlags);
    }

    public AcceptExchangeEntry prepare(final BiConsumer<Result<ClientConnection<?>>, Proactor> completion,
                                       final int descriptor,
                                       final int acceptFlags) {
        this.descriptor = descriptor;
        this.acceptFlags = acceptFlags;
        clientAddress.reset();
        return super.prepare(completion);
    }
}

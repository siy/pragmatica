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

import org.pfj.io.uring.struct.raw.SubmitQueueEntry;
import org.pfj.io.NativeFailureType;
import org.pfj.io.async.Submitter;
import org.pfj.io.async.file.FileDescriptor;
import org.pfj.io.async.net.SocketAddress;
import org.pfj.io.uring.struct.ExternalRawStructure;
import org.pfj.io.uring.struct.offheap.OffHeapSocketAddress;
import org.pfj.io.uring.utils.PlainObjectPool;
import org.pfj.lang.Result;

import java.util.function.BiConsumer;

import static org.pfj.io.uring.AsyncOperation.IORING_OP_CONNECT;
import static org.pfj.lang.Result.success;

public class ConnectExchangeEntry extends AbstractExchangeEntry<ConnectExchangeEntry, FileDescriptor> {
    private OffHeapSocketAddress<SocketAddress<?>, ExternalRawStructure<?>> clientAddress;
    private byte flags;
    private FileDescriptor descriptor;

    protected ConnectExchangeEntry(final PlainObjectPool<ConnectExchangeEntry> pool) {
        super(IORING_OP_CONNECT, pool);
    }

    @Override
    protected void doAccept(final int res, final int flags, final Submitter submitter) {
        completion.accept(res < 0
                ? NativeFailureType.result(res)
                : success(descriptor),
            submitter);

        clientAddress.dispose();
        clientAddress = null;
    }

    @Override
    public SubmitQueueEntry apply(final SubmitQueueEntry entry) {
        return super.apply(entry)
            .fd(descriptor.descriptor())
            .addr(clientAddress.sockAddrPtr())
            .off(clientAddress.sockAddrSize());
    }

    public ConnectExchangeEntry prepare(final BiConsumer<Result<FileDescriptor>, Submitter> completion,
                                        final FileDescriptor descriptor,
                                        final OffHeapSocketAddress<SocketAddress<?>, ExternalRawStructure<?>> clientAddress,
                                        final byte flags) {
        this.clientAddress = clientAddress;
        this.descriptor = descriptor;
        this.flags = flags;

        return super.prepare(completion);
    }
}

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
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapSocketAddress;
import org.pragmatica.io.async.uring.struct.raw.SubmitQueueEntry;
import org.pragmatica.io.async.uring.utils.PlainObjectPool;
import org.pragmatica.lang.Result;

import java.util.function.BiConsumer;

import static org.pragmatica.io.async.uring.AsyncOperation.IORING_OP_CONNECT;
import static org.pragmatica.lang.Result.success;

public class ConnectExchangeEntry extends AbstractExchangeEntry<ConnectExchangeEntry, FileDescriptor> {
    private OffHeapSocketAddress clientAddress;
    private byte flags;
    private FileDescriptor descriptor;

    public ConnectExchangeEntry(PlainObjectPool<ConnectExchangeEntry> pool) {
        super(IORING_OP_CONNECT);
    }

    @Override
    protected void doAccept(int res, int flags, Proactor proactor) {
        completion.accept(res < 0
                          ? SystemError.result(res)
                          : success(descriptor),
                          proactor);

        clientAddress.dispose();
        clientAddress = null;
    }

    @Override
    public SubmitQueueEntry apply(SubmitQueueEntry entry) {
        return super.apply(entry)
                    .fd(descriptor.descriptor())
                    .flags(flags)
                    .addr(clientAddress.sockAddrPtr())
                    .off(clientAddress.sockAddrSize());
    }

    public ConnectExchangeEntry prepare(BiConsumer<Result<FileDescriptor>, Proactor> completion, FileDescriptor descriptor,
                                        OffHeapSocketAddress clientAddress, byte flags) {
        this.clientAddress = clientAddress;
        this.descriptor = descriptor;
        this.flags = flags;

        return super.prepare(completion);
    }
}

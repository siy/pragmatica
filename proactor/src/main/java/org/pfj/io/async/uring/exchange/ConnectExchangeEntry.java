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
import org.pfj.io.async.SystemError;
import org.pfj.io.async.file.FileDescriptor;
import org.pfj.io.async.net.SocketAddress;
import org.pfj.io.async.uring.struct.ExternalRawStructure;
import org.pfj.io.async.uring.struct.offheap.OffHeapSocketAddress;
import org.pfj.io.async.uring.struct.raw.SubmitQueueEntry;
import org.pfj.io.async.uring.utils.PlainObjectPool;
import org.pfj.lang.Result;

import java.util.function.BiConsumer;

import static org.pfj.io.async.uring.AsyncOperation.IORING_OP_CONNECT;
import static org.pfj.lang.Result.success;

public class ConnectExchangeEntry extends AbstractExchangeEntry<ConnectExchangeEntry, FileDescriptor> {
    private OffHeapSocketAddress<SocketAddress<?>, ExternalRawStructure<?>> clientAddress;
    private byte flags;
    private FileDescriptor descriptor;

    protected ConnectExchangeEntry(PlainObjectPool<ConnectExchangeEntry> pool) {
        super(IORING_OP_CONNECT, pool);
    }

    @Override
    protected void doAccept(int res,  int flags,  Proactor proactor) {
        completion.accept(res < 0
                          ? SystemError.result(res)
                          : success(descriptor),
                          proactor);

        clientAddress.dispose();
        clientAddress = null;
    }

    @Override
    public SubmitQueueEntry apply( SubmitQueueEntry entry) {
        return super.apply(entry)
                    .fd(descriptor.descriptor())
                    .addr(clientAddress.sockAddrPtr())
                    .off(clientAddress.sockAddrSize());
    }

    public ConnectExchangeEntry prepare( BiConsumer<Result<FileDescriptor>, Proactor> completion, FileDescriptor descriptor, OffHeapSocketAddress<SocketAddress<?>, ExternalRawStructure<?>> clientAddress, byte flags) {
        this.clientAddress = clientAddress;
        this.descriptor = descriptor;
        this.flags = flags;

        return super.prepare(completion);
    }
}
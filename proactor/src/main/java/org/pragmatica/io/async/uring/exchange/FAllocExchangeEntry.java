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
import org.pragmatica.io.async.uring.struct.raw.SQEntry;
import org.pragmatica.io.async.uring.utils.PlainObjectPool;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.util.function.BiConsumer;

import static org.pragmatica.io.async.uring.AsyncOperation.IORING_OP_FALLOCATE;
import static org.pragmatica.lang.Unit.unitResult;

public class FAllocExchangeEntry extends AbstractExchangeEntry<FAllocExchangeEntry, Unit> {
    private int descriptor;
    private int allocFlags;
    private long offset;
    private long len;
    private byte flags;

    protected FAllocExchangeEntry(final PlainObjectPool<FAllocExchangeEntry> pool) {
        super(IORING_OP_FALLOCATE, pool);
    }

    @Override
    protected void doAccept(final int res, final int flags, final Proactor proactor) {
        completion.accept(res == 0 ? unitResult() : SystemError.result(res), proactor);
    }

    public FAllocExchangeEntry prepare(final BiConsumer<Result<Unit>, Proactor> completion,
                                       final int descriptor,
                                       final int allocFlags,
                                       final long offset,
                                       final long len,
                                       final byte flags) {
        this.descriptor = descriptor;
        this.allocFlags = allocFlags;
        this.offset = offset;
        this.len = len;
        this.flags = flags;

        return super.prepare(completion);
    }

    @Override
    public SQEntry apply(final SQEntry entry) {
        return super.apply(entry)
                    .flags(flags)
                    .addr(len)
                    .len(allocFlags)
                    .off(offset)
                    .fd(descriptor);
    }
}

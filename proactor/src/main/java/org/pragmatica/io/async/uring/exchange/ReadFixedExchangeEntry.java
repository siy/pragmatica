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
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.uring.struct.raw.SQEntry;
import org.pragmatica.io.async.uring.utils.PlainObjectPool;
import org.pragmatica.io.async.util.allocator.FixedBuffer;
import org.pragmatica.lang.Result;

import java.util.function.BiConsumer;

import static org.pragmatica.io.async.uring.AsyncOperation.READ_FIXED;

/**
 * Exchange entry for {@code readFixed} request.
 */
public class ReadFixedExchangeEntry extends AbstractExchangeEntry<ReadFixedExchangeEntry, SizeT> {
    private int descriptor;
    private byte flags;
    private FixedBuffer buffer;
    private long offset;

    protected ReadFixedExchangeEntry(PlainObjectPool<ReadFixedExchangeEntry> pool) {
        super(READ_FIXED, pool);
    }

    @Override
    protected void doAccept(int res, int flags, Proactor proactor) {
        if (res > 0) {
            buffer.used(res);
        }
        completion.accept(bytesReadToResult(res), proactor);
    }

    @Override
    public SQEntry apply(SQEntry entry) {
        return super.apply(entry)
                    .fd(descriptor)
                    .flags(flags)
                    .addr(buffer.address())
                    .len(buffer.size())
                    .off(offset)
                    .bufIndex((short) 0);
    }

    public ReadFixedExchangeEntry prepare(BiConsumer<Result<SizeT>, Proactor> completion,
                                          int descriptor,
                                          FixedBuffer buffer,
                                          long offset,
                                          byte flags) {
        this.descriptor = descriptor;
        this.flags = flags;
        this.buffer = buffer;
        this.offset = offset;
        return super.prepare(completion);
    }
}

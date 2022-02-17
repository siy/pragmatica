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
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.uring.struct.raw.SQEntry;
import org.pragmatica.io.async.uring.utils.PlainObjectPool;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.lang.Result;

import java.util.function.BiConsumer;

import static org.pragmatica.io.async.uring.AsyncOperation.IORING_OP_WRITE;

public class WriteExchangeEntry extends AbstractExchangeEntry<WriteExchangeEntry, SizeT> {
    private int descriptor;
    private byte flags;
    private OffHeapSlice buffer;
    private long offset;

    protected WriteExchangeEntry(PlainObjectPool<WriteExchangeEntry> pool) {
        super(IORING_OP_WRITE, pool);
    }

    @Override
    protected void doAccept(int res, int flags, Proactor proactor) {
        completion.accept(byteCountToResult(res), proactor);
    }

    @Override
    public SQEntry apply(SQEntry entry) {
        return super.apply(entry)
                    .fd(descriptor)
                    .flags(flags)
                    .addr(buffer.address())
                    .len(buffer.used())
                    .off(offset);
    }

    public WriteExchangeEntry prepare(BiConsumer<Result<SizeT>, Proactor> completion,
                                      int descriptor,
                                      OffHeapSlice buffer,
                                      long offset,
                                      byte flags) {
        this.descriptor = descriptor;
        this.flags = flags;
        this.buffer = buffer;
        this.offset = offset;
        return super.prepare(completion);
    }

    private Result<SizeT> byteCountToResult(int res) {
        return res > 0
               ? sizeResult(res)
               : SystemError.result(res);
    }
}

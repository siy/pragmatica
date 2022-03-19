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
import org.pragmatica.io.async.uring.struct.offheap.OffHeapIoVector;
import org.pragmatica.io.async.uring.struct.raw.SQEntry;
import org.pragmatica.io.async.uring.utils.PlainObjectPool;
import org.pragmatica.lang.Result;

import java.util.function.BiConsumer;

import static org.pragmatica.io.async.uring.AsyncOperation.IORING_OP_READV;

/**
 * Exchange entry for {@code readVector} request.
 */
public class ReadVectorExchangeEntry extends AbstractExchangeEntry<ReadVectorExchangeEntry, SizeT> {
    private static final Result<SizeT> EOF_RESULT = SystemError.ENODATA.result();

    private OffHeapIoVector ioVector;
    private byte flags;
    private int descriptor;
    private long offset;

    protected ReadVectorExchangeEntry(PlainObjectPool<ReadVectorExchangeEntry> pool) {
        super(IORING_OP_READV, pool);
    }

    @Override
    protected void doAccept(int res, int flags, Proactor proactor) {
        completion.accept(bytesReadToResult(res), proactor);
        ioVector.dispose();
        ioVector = null;
    }

    @Override
    public SQEntry apply(SQEntry entry) {
        return super.apply(entry)
                    .flags(flags)
                    .fd(descriptor)
                    .addr(ioVector.address())
                    .len(ioVector.length())
                    .off(offset);
    }

    public ReadVectorExchangeEntry prepare(BiConsumer<Result<SizeT>, Proactor> completion,
                                           int descriptor,
                                           long offset,
                                           byte flags,
                                           OffHeapIoVector ioVector) {
        this.descriptor = descriptor;
        this.offset = offset;
        this.flags = flags;
        this.ioVector = ioVector;
        return super.prepare(completion);
    }

    private Result<SizeT> bytesReadToResult(int res) {
        return res == 0 ? EOF_RESULT
                        : res > 0 ? sizeResult(res)
                                  : SystemError.result(res);
    }
}

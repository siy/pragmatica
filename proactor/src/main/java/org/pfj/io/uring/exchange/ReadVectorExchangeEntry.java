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
import org.pfj.io.async.common.SizeT;
import org.pfj.io.uring.struct.offheap.OffHeapIoVector;
import org.pfj.io.uring.utils.PlainObjectPool;
import org.pfj.lang.Result;

import java.util.function.BiConsumer;

import static org.pfj.io.uring.AsyncOperation.IORING_OP_READV;

public class ReadVectorExchangeEntry extends AbstractExchangeEntry<ReadVectorExchangeEntry, SizeT> {
    private static final Result<SizeT> EOF_RESULT = NativeFailureType.ENODATA.result();

    private OffHeapIoVector ioVector;
    private byte flags;
    private int descriptor;
    private long offset;

    protected ReadVectorExchangeEntry(final PlainObjectPool<ReadVectorExchangeEntry> pool) {
        super(IORING_OP_READV, pool);
    }

    @Override
    protected void doAccept(final int res, final int flags, final Submitter submitter) {
        completion.accept(bytesReadToResult(res), submitter);
        ioVector.dispose();
        ioVector = null;
    }

    @Override
    public SubmitQueueEntry apply(final SubmitQueueEntry entry) {
        return super.apply(entry)
                    .flags(flags)
                    .fd(descriptor)
                    .addr(ioVector.address())
                    .len(ioVector.length())
                    .off(offset);
    }

    public ReadVectorExchangeEntry prepare(final BiConsumer<Result<SizeT>, Submitter> completion,
                                           final int descriptor,
                                           final long offset,
                                           final byte flags,
                                           final OffHeapIoVector ioVector) {
        this.descriptor = descriptor;
        this.offset = offset;
        this.flags = flags;
        this.ioVector = ioVector;
        return super.prepare(completion);
    }

    private Result<SizeT> bytesReadToResult(final int res) {
        return res == 0 ? EOF_RESULT
                        : res > 0 ? sizeResult(res)
                                  : NativeFailureType.result(res);
    }
}

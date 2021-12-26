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
import org.pfj.io.async.common.SizeT;
import org.pfj.io.async.file.SpliceDescriptor;
import org.pfj.io.async.uring.Bitmask;
import org.pfj.io.async.uring.struct.raw.SubmitQueueEntry;
import org.pfj.io.async.uring.utils.PlainObjectPool;
import org.pfj.lang.Result;

import java.util.function.BiConsumer;

import static org.pfj.io.async.uring.AsyncOperation.IORING_OP_SPLICE;

public class SpliceExchangeEntry extends AbstractExchangeEntry<SpliceExchangeEntry, SizeT> {
    private SpliceDescriptor descriptor;
    private byte flags;

    protected SpliceExchangeEntry(final PlainObjectPool<SpliceExchangeEntry> pool) {
        super(IORING_OP_SPLICE, pool);
    }

    @Override
    protected void doAccept(final int res, final int flags, final Proactor proactor) {
        completion.accept(byteCountToResult(res), proactor);
    }

    @Override
    public SubmitQueueEntry apply(final SubmitQueueEntry entry) {
        return super.apply(entry)
                    .flags(flags)
                    .fd(descriptor.toDescriptor().descriptor())
                    .len((int) descriptor.bytesToCopy().value())    //TODO: investigate len
                    .off(descriptor.toOffset().value())
                    .spliceFdIn(descriptor.fromDescriptor().descriptor())
                    .spliceOffIn(descriptor.fromOffset().value())
                    .spliceFlags(Bitmask.combine(descriptor.flags()));
    }

    public SpliceExchangeEntry prepare(final BiConsumer<Result<SizeT>, Proactor> completion,
                                       final SpliceDescriptor descriptor,
                                       final byte flags) {
        this.flags = flags;
        this.descriptor = descriptor;
        return super.prepare(completion);
    }

    private Result<SizeT> byteCountToResult(final int res) {
        return res > 0
               ? sizeResult(res)
               : SystemError.result(res);
    }
}

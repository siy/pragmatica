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
import org.pragmatica.io.async.file.SpliceDescriptor;
import org.pragmatica.io.async.uring.Bitmask;
import org.pragmatica.io.async.uring.struct.raw.SQEntry;
import org.pragmatica.io.async.uring.utils.PlainObjectPool;
import org.pragmatica.lang.Result;

import java.util.function.BiConsumer;

import static org.pragmatica.io.async.uring.AsyncOperation.SPLICE;

/**
 * Exchange entry for {@code splice} request.
 */
public class SpliceExchangeEntry extends AbstractExchangeEntry<SpliceExchangeEntry, SizeT> {
    private SpliceDescriptor descriptor;
    private byte flags;

    protected SpliceExchangeEntry(final PlainObjectPool<SpliceExchangeEntry> pool) {
        super(SPLICE, pool);
    }

    @Override
    protected void doAccept(final int res, final int flags, final Proactor proactor) {
        completion.accept(byteCountToResult(res), proactor);
    }

    @Override
    public SQEntry apply(final SQEntry entry) {
        return super.apply(entry)
                    .flags(flags)
                    .fd(descriptor.toDescriptor().descriptor())
                    .len((int) descriptor.bytesToCopy().value())
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
}

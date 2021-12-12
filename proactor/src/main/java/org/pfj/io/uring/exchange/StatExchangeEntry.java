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

import org.pfj.io.async.file.stat.FileStat;
import org.pfj.io.uring.struct.raw.SubmitQueueEntry;
import org.pfj.io.NativeFailureType;
import org.pfj.io.async.Submitter;
import org.pfj.io.uring.struct.offheap.OffHeapCString;
import org.pfj.io.uring.struct.offheap.OffHeapFileStat;
import org.pfj.io.uring.utils.PlainObjectPool;
import org.pfj.lang.Result;

import java.util.function.BiConsumer;

import static org.pfj.io.uring.AsyncOperation.IORING_OP_STATX;
import static org.pfj.lang.Result.success;

public class StatExchangeEntry extends AbstractExchangeEntry<StatExchangeEntry, FileStat> {
    private final OffHeapFileStat fileStat = OffHeapFileStat.fileStat();
    private OffHeapCString rawPath;
    private int descriptor;
    private int statFlags;
    private int statMask;

    protected StatExchangeEntry(final PlainObjectPool<StatExchangeEntry> pool) {
        super(IORING_OP_STATX, pool);
    }

    @Override
    protected void doAccept(final int res, final int flags, final Submitter submitter) {
        completion.accept(res < 0
                ? NativeFailureType.result(res)
                : success(fileStat.extract()),
            submitter);
        fileStat.dispose();
        rawPath.dispose();
        rawPath = null;
    }

    @Override
    public void close() {
        fileStat.dispose();

        if (rawPath != null) {
            rawPath.dispose();
        }
    }

    @Override
    public SubmitQueueEntry apply(final SubmitQueueEntry entry) {
        return super.apply(entry)
            .fd(descriptor)
            .addr(rawPath.address())
            .len(statMask)
            .off(fileStat.address())
            .statxFlags(statFlags);
    }

    public StatExchangeEntry prepare(final BiConsumer<Result<FileStat>, Submitter> completion,
                                     final int descriptor,
                                     final int statFlags,
                                     final int statMask,
                                     final OffHeapCString rawPath) {
        this.descriptor = descriptor;
        this.statFlags = statFlags;
        this.statMask = statMask;
        this.rawPath = rawPath;

        fileStat.clear();

        return super.prepare(completion);
    }
}

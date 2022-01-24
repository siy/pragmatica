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
import org.pragmatica.io.async.file.stat.FileStat;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapCString;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapFileStat;
import org.pragmatica.io.async.uring.struct.raw.SubmitQueueEntry;
import org.pragmatica.io.async.uring.utils.PlainObjectPool;
import org.pragmatica.lang.Result;

import java.util.function.BiConsumer;

import static org.pragmatica.io.async.uring.AsyncOperation.IORING_OP_STATX;
import static org.pragmatica.lang.Result.success;

public class StatExchangeEntry extends AbstractExchangeEntry<StatExchangeEntry, FileStat> {
    private final OffHeapFileStat fileStat = OffHeapFileStat.fileStat();
    private OffHeapCString rawPath;
    private int descriptor;
    private int statFlags;
    private int statMask;

    public StatExchangeEntry(PlainObjectPool<StatExchangeEntry> pool) {
        super(IORING_OP_STATX);
    }

    @Override
    protected void doAccept(int res, int flags, Proactor proactor) {
        completion.accept(res < 0
                          ? SystemError.result(res)
                          : success(fileStat.extract()),
                          proactor);
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
    public SubmitQueueEntry apply(SubmitQueueEntry entry) {
        return super.apply(entry)
                    .fd(descriptor)
                    .addr(rawPath.address())
                    .len(statMask)
                    .off(fileStat.address())
                    .statxFlags(statFlags);
    }

    public StatExchangeEntry prepare(BiConsumer<Result<FileStat>, Proactor> completion,
                                     int descriptor,
                                     int statFlags,
                                     int statMask,
                                     OffHeapCString rawPath) {
        this.descriptor = descriptor;
        this.statFlags = statFlags;
        this.statMask = statMask;
        this.rawPath = rawPath;

        fileStat.clear();

        return super.prepare(completion);
    }
}

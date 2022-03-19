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

import static org.pragmatica.io.async.uring.AsyncOperation.IORING_OP_FSYNC;
import static org.pragmatica.lang.Unit.unitResult;

/**
 * Exchange entry for {@code fsync} request.
 */
public class FSyncExchangeEntry extends AbstractExchangeEntry<FSyncExchangeEntry, Unit> {
    private int descriptor;
    private int fsyncFlags;
    private byte flags;

    protected FSyncExchangeEntry(final PlainObjectPool<FSyncExchangeEntry> pool) {
        super(IORING_OP_FSYNC, pool);
    }

    @Override
    protected void doAccept(final int res, final int flags, final Proactor proactor) {
        completion.accept(res == 0 ? unitResult() : SystemError.result(res), proactor);
    }

    public FSyncExchangeEntry prepare(final BiConsumer<Result<Unit>, Proactor> completion,
                                      final int descriptor,
                                      final int fsyncFlags,
                                      final byte flags) {
        this.descriptor = descriptor;
        this.fsyncFlags = fsyncFlags;
        this.flags = flags;

        return super.prepare(completion);
    }

    @Override
    public SQEntry apply(final SQEntry entry) {
        return super.apply(entry)
                    .flags(flags)
                    .fsyncFlags(fsyncFlags)
                    .fd(descriptor);
    }
}

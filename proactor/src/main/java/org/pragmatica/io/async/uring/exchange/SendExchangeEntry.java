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
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.lang.Result;

import java.util.function.BiConsumer;

import static org.pragmatica.io.async.uring.AsyncOperation.SEND;

/**
 * Exchange entry for {@code send} request.
 */
public class SendExchangeEntry extends AbstractExchangeEntry<SendExchangeEntry, SizeT> {
    private int msgFlags;
    private byte flags;
    private int descriptor;
    private OffHeapSlice buffer;

    protected SendExchangeEntry(PlainObjectPool<SendExchangeEntry> pool) {
        super(SEND, pool);
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
                    .msgFlags(msgFlags)
                    .addr(buffer.address())
                    .len(buffer.used());
    }

    public SendExchangeEntry prepare(BiConsumer<Result<SizeT>, Proactor> completion,
                                     int descriptor,
                                     OffHeapSlice buffer,
                                     int msgFlags,
                                     byte flags) {
        this.buffer = buffer;
        this.descriptor = descriptor;
        this.msgFlags = msgFlags;
        this.flags = flags;

        return super.prepare(completion);
    }
}

/*
 *  Copyright (c) 2020-2022 Sergiy Yevtushenko.
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
 */

package org.pragmatica.io.async.uring.exchange;

import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapIoVector;
import org.pragmatica.io.async.uring.struct.raw.SQEntry;
import org.pragmatica.lang.Result;

import java.util.function.BiConsumer;

import static org.pragmatica.io.async.uring.AsyncOperation.READV;

/**
 * Exchange entry for {@code readVector} request.
 */
public class ReadVectorExchangeEntry extends AbstractExchangeEntry<ReadVectorExchangeEntry, SizeT> {
    private OffHeapIoVector ioVector;
    private byte flags;
    private int descriptor;
    private long offset;

    protected ReadVectorExchangeEntry() {
        super(READV);
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
}

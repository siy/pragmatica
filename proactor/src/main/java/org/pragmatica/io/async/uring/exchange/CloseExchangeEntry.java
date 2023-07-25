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
import org.pragmatica.io.async.SystemError;
import org.pragmatica.io.async.uring.struct.raw.SQEntry;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.util.function.BiConsumer;

import static org.pragmatica.io.async.uring.AsyncOperation.CLOSE;
import static org.pragmatica.lang.Unit.unitResult;

/**
 * Exchange entry for {@code close} request.
 */
public class CloseExchangeEntry extends AbstractExchangeEntry<CloseExchangeEntry, Unit> {
    private int descriptor;
    private byte flags;

    protected CloseExchangeEntry() {
        super(CLOSE);
    }

    @Override
    protected void doAccept(final int res, final int flags, final Proactor proactor) {
        completion.accept(res == 0 ? unitResult() : SystemError.result(res), proactor);
    }

    @Override
    public SQEntry apply(final SQEntry entry) {
        return super.apply(entry)
                    .flags(flags)
                    .fd(descriptor);
    }

    public CloseExchangeEntry prepare(final BiConsumer<Result<Unit>, Proactor> completion,
                                      final int descriptor,
                                      final byte flags) {
        this.descriptor = descriptor;
        this.flags = flags;
        return super.prepare(completion);
    }
}

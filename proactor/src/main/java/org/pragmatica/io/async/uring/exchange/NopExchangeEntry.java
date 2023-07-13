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
import org.pragmatica.io.async.uring.utils.PlainObjectPool;
import org.pragmatica.lang.Unit;

import static org.pragmatica.io.async.uring.AsyncOperation.NOP;
import static org.pragmatica.lang.Unit.unitResult;

/**
 * Exchange entry for {@code nop} request.
 */
public class NopExchangeEntry extends AbstractExchangeEntry<NopExchangeEntry, Unit> {
    protected NopExchangeEntry(final PlainObjectPool<NopExchangeEntry> pool) {
        super(NOP, pool);
    }

    @Override
    protected void doAccept(final int result, final int flags, final Proactor proactor) {
        completion.accept(unitResult(), proactor);
    }
}

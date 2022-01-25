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
import org.pragmatica.io.async.uring.AsyncOperation;
import org.pragmatica.io.async.uring.CompletionHandler;
import org.pragmatica.io.async.uring.struct.raw.SubmitQueueEntry;
import org.pragmatica.io.async.uring.utils.ObjectHeap;
import org.pragmatica.io.async.uring.utils.PlainObjectPool;
import org.pragmatica.lang.Result;

import java.util.function.BiConsumer;

import static org.pragmatica.io.async.common.SizeT.sizeT;
import static org.pragmatica.lang.Result.success;

@SuppressWarnings("rawtypes")
public abstract class AbstractExchangeEntry<T extends AbstractExchangeEntry<T, R>, R> implements ExchangeEntry<T> {
    private static final int RESULT_SIZET_POOL_SIZE = 65536;
    @SuppressWarnings("rawtypes")
    private static final Result[] RESULT_SIZET_POOL;

    static {
        RESULT_SIZET_POOL = new Result[RESULT_SIZET_POOL_SIZE];

        for (int i = 0; i < RESULT_SIZET_POOL.length; i++) {
            RESULT_SIZET_POOL[i] = success(sizeT(i));
        }
    }

    private final PlainObjectPool pool;
    private final AsyncOperation operation;
    public T next;
    private int key;
    protected BiConsumer<Result<R>, Proactor> completion;

    protected AbstractExchangeEntry(AsyncOperation operation, PlainObjectPool pool) {
        this.operation = operation;
        this.pool = pool;
    }

    @SuppressWarnings("unchecked")
    public void release() {
        completion = null;
        pool.release(this);
    }

    @Override
    public final void accept(int result, int flags, Proactor proactor) {
        doAccept(result, flags, proactor);
        release();
    }

    protected abstract void doAccept(int result, int flags, Proactor proactor);

    @Override
    public void close() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public T register(ObjectHeap<CompletionHandler> heap) {
        key = heap.allocKey(this);
        return (T) this;
    }

    @Override
    public SubmitQueueEntry apply(SubmitQueueEntry entry) {
        return entry.userData(key)
                    .opcode(operation.opcode());
    }

    @SuppressWarnings("unchecked")
    public T prepare(BiConsumer<Result<R>, Proactor> completion) {
        this.completion = completion;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    protected static Result<SizeT> sizeResult(int res) {
        return res < RESULT_SIZET_POOL.length
               ? RESULT_SIZET_POOL[res]
               : success(sizeT(res));
    }
}

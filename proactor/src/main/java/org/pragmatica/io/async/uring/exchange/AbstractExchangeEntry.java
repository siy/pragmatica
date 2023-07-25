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
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.uring.AsyncOperation;
import org.pragmatica.io.async.uring.struct.raw.SQEntry;
import org.pragmatica.io.async.uring.utils.ExchangeEntryPool;
import org.pragmatica.lang.Result;

import java.util.function.BiConsumer;

import static org.pragmatica.io.async.common.SizeT.sizeT;
import static org.pragmatica.lang.Result.success;

/**
 * Base type for containers used to store callback information for in-flight requests.
 */
public abstract class AbstractExchangeEntry<T extends AbstractExchangeEntry<T, R>, R> implements ExchangeEntry<T> {
    private static final int RESULT_SIZET_POOL_SIZE = 65536;
    @SuppressWarnings("rawtypes")
    private static final Result[] RESULT_SIZET_POOL;
    private static final Result<SizeT> EOF_RESULT = SystemError.ENODATA.result();

    static {
        RESULT_SIZET_POOL = new Result[RESULT_SIZET_POOL_SIZE];

        for (int i = 0; i < RESULT_SIZET_POOL.length; i++) {
            RESULT_SIZET_POOL[i] = success(sizeT(i));
        }
    }

    private final AsyncOperation operation;
    private volatile T next;
    private int key;

    private ExchangeEntryPool<T> pool;

    protected volatile BiConsumer<Result<R>, Proactor> completion = null;

    protected AbstractExchangeEntry(AsyncOperation operation) {
        this.operation = operation;
    }

    @Override
    public boolean isUsable() {
        return completion != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void accept(int result, int flags, Proactor proactor) {
        doAccept(result, flags, proactor);
        completion = null;
        pool.release((T) this);
    }

    protected abstract void doAccept(int result, int flags, Proactor proactor);

    @Override
    public void close() {
    }

    @Override
    public int key() {
        return key;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T key(int key, ExchangeEntryPool<T> pool) {
        this.key = key;
        this.pool = pool;
        return (T) this;
    }

    @Override
    public T next() {
        return next;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T next(T next) {
        this.next = next;
        return (T) this;
    }

    @Override
    public SQEntry apply(SQEntry entry) {
        return entry.userData(key())
                    .opcode(operation.opcode());
    }

    @SuppressWarnings("unchecked")
    public T prepare(BiConsumer<Result<R>, Proactor> completion) {
        if (this.completion != null) {
            throw new IllegalStateException("Entry is already in use");
        }

        this.completion = completion;
        return (T) this;
    }

    protected static Result<SizeT> byteCountToResult(int res) {
        return res > 0
               ? sizeResult(res)
               : SystemError.result(res);
    }

    protected static Result<SizeT> bytesReadToResult(int res) {
        return res == 0 ? EOF_RESULT
                        : res > 0 ? sizeResult(res)
                                  : SystemError.result(res);
    }

    @SuppressWarnings("unchecked")
    protected static Result<SizeT> sizeResult(int res) {
        return res < RESULT_SIZET_POOL.length
               ? RESULT_SIZET_POOL[res]
               : success(sizeT(res));
    }
}

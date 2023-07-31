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
import org.pragmatica.io.async.uring.struct.raw.CQEntry;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.pragmatica.io.async.uring.exchange.ExchangeEntry.exchangeEntry;

/**
 * Factory for the different types of exchange entries.
 * <p>
 * This implementation uses pools of the instances for each type of exchange entry. This approach eliminates pressure on GC when huge amount of
 * requests are issued.
 */
public class ExchangeEntryPool {
    private static class ExchangeEntryCell {

        final ExchangeEntry<?> entry;

        final AtomicBoolean inUse = new AtomicBoolean(false);
        private ExchangeEntryCell(int index) {
            this.entry = exchangeEntry(index);
        }

    }

    private static final int INITIAL_POOL_SIZE = 2048;

    private final AtomicInteger probeCount = new AtomicInteger(0);
    private final transient Object lock = new Object();
    private final AtomicInteger usedCounter = new AtomicInteger(0);
//    private final AtomicInteger maxUsedCounter = new AtomicInteger(0);
//    private final AtomicInteger maxRetries = new AtomicInteger(0);
    private transient volatile ExchangeEntryCell[] array;

    private ExchangeEntryCell[] getArray() {
        return array;
    }

    /**
     * Sets the array.
     */
    private void setArray(ExchangeEntryCell[] a) {
        array = a;
    }

    private ExchangeEntryPool() {
        setArray(populate(new ExchangeEntryCell[INITIAL_POOL_SIZE]));
    }

    private ExchangeEntryCell[] populate(ExchangeEntryCell[] objects) {
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] != null) {
                continue;
            }
            objects[i] = new ExchangeEntryCell(i);
        }
        return objects;
    }

    private static ExchangeEntryCell elementAt(ExchangeEntryCell[] a, int index) {
        return a[index];
    }

    public static ExchangeEntryPool exchangeEntryPool() {
        return new ExchangeEntryPool();
    }

    public int size() {
        return getArray().length;
    }

    @SuppressWarnings("unchecked")
    public <T> ExchangeEntry<T> lookup(int key) {
        return (ExchangeEntry<T>) ExchangeEntryPool.elementAt(getArray(), key).entry;
    }

    @SuppressWarnings("unchecked")
    public <T> ExchangeEntry<T> acquire() {
        var array = getArray();
        var maxRetries = size() - usedCounter.get();

        for (int i = 0; i < maxRetries; i++) {
            var index = probeCount.incrementAndGet() & (array.length - 1);
            var element = ExchangeEntryPool.elementAt(array, index);

            if (element.inUse.get()) {
                continue;
            }

            if (element.inUse.compareAndSet(false, true)) {
//                usedCounter.incrementAndGet();
//                while (!this.maxUsedCounter.compareAndSet(this.maxUsedCounter.get(), this.usedCounter.get())) {
//                    // spin
//                }
//                while (!this.maxRetries.compareAndSet(this.maxRetries.get(), Math.max(this.maxRetries.get(), i))) {
//                    // spin
//                }
                return (ExchangeEntry<T>) element.entry;
            }
        }

        synchronized (lock) {
            var es = getArray();
            var len = es.length;
            es = Arrays.copyOf(es, len * 2);
            populate(es);
            setArray(es);

            return acquire();
        }
    }

    private <T> void release(ExchangeEntry<T> entry) {
        ExchangeEntryPool.elementAt(getArray(), entry.key()).inUse.lazySet(false);
        this.usedCounter.decrementAndGet();
    }

    public void clear() {
        synchronized (lock) {
            var array = getArray();

            for (int i = 0; i < array.length; i++) {
                elementAt(array, i).entry.close();
            }
        }
    }

//    public int maxRetries() {
//        return maxRetries.get();
//    }

//    public int maxUsed() {
//        return maxUsedCounter.get();
//    }

    public void completeRequest(CQEntry cqEntry, Proactor proactor) {
        int key = (int) cqEntry.userData();
        var entry = lookup(key);

        entry.processCompletion(cqEntry, proactor);

        release(entry);
    }
}

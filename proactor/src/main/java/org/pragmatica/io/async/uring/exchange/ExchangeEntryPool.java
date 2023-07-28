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
import java.util.concurrent.atomic.AtomicMarkableReference;

import static org.pragmatica.io.async.uring.exchange.ExchangeEntry.exchangeEntry;

/**
 * Factory for the different types of exchange entries.
 * <p>
 * This implementation uses pools of the instances for each type of exchange entry. This approach eliminates pressure on GC when huge amount of
 * requests are issued.
 */
public class ExchangeEntryPool {
    private static final int INITIAL_POOL_SIZE = 1024;
    private static final int MAX_RETRIES = 7;
    private static final Xoshiro256PlusPlus random = Xoshiro256PlusPlus.xoshiro256PlusPlus();
    private final transient Object lock = new Object();

    private transient volatile Object[] array;

    private Object[] getArray() {
        return array;
    }

    /**
     * Sets the array.
     */
    private void setArray(Object[] a) {
        array = a;
    }

    private ExchangeEntryPool() {
        setArray(populate(new Object[INITIAL_POOL_SIZE]));
    }

    private Object[] populate(Object[] objects) {
        for (int i = 0; i < objects.length; i++) {
            objects[i] = new AtomicMarkableReference<>(exchangeEntry(i), false);
        }
        return objects;
    }

    @SuppressWarnings("unchecked")
    private static <E> AtomicMarkableReference<ExchangeEntry<E>> elementAt(Object[] a, int index) {
        return (AtomicMarkableReference<ExchangeEntry<E>>) a[index];
    }

    public static ExchangeEntryPool exchangeEntryPool() {
        return new ExchangeEntryPool();
    }

    public <T> ExchangeEntry<T> lookup(int key) {
        return ExchangeEntryPool.<T>elementAt(getArray(), key).getReference();
    }

    public <T> ExchangeEntry<T> acquire() {
        var array = getArray();

        for (int i = 0; i < MAX_RETRIES; i++) {
            var index = (int) (random.next() & (array.length - 1));
            var element = ExchangeEntryPool.<T>elementAt(array, index);

            if (element.isMarked()) {
                continue;
            }

            if (element.attemptMark(element.getReference(), true)) {
                return element.getReference();
            }
        }

        synchronized (lock) {
            Object[] es = getArray();
            int len = es.length;
            es = Arrays.copyOf(es, len * 2);
            populate(es);
            setArray(es);

            return acquire();
        }
    }

    private <T> void release(ExchangeEntry<T> entry) {
        ExchangeEntryPool.<T>elementAt(getArray(), entry.key()).attemptMark(entry, false);
    }

    public void clear() {
        synchronized (lock) {
            var array = getArray();

            for (int i = 0; i < array.length; i++) {
                var entry = elementAt(array, i).getReference();
                entry.close();
                elementAt(array, i).set(null, false);
            }
        }
    }

    public void completeRequest(CQEntry cqEntry, Proactor proactor) {
        int key = (int) cqEntry.userData();
        var entry = lookup(key);

        entry.processCompletion(cqEntry, proactor);

        release(entry);
    }
}

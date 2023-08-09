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
import java.util.concurrent.atomic.AtomicReference;

import static org.pragmatica.io.async.uring.exchange.ExchangeEntry.exchangeEntry;

public class HybridExchangeEntryPool implements ExchangeEntryPool {
    private static class ExchangeEntryCell {
        final ExchangeEntry<?> entry;

        ExchangeEntryCell next;

        private ExchangeEntryCell(int index) {
            this.entry = exchangeEntry(index);
        }
    }

    private final AtomicReference<ExchangeEntryCell> head = new AtomicReference<>();

    private static final int INITIAL_POOL_SIZE = 2048;

    private transient volatile ExchangeEntryCell[] array;
    private final transient Object lock = new Object();

    private ExchangeEntryCell[] getArray() {
        return array;
    }

    /**
     * Sets the array.
     */
    private void setArray(ExchangeEntryCell[] a) {
        array = a;
    }

    HybridExchangeEntryPool() {
        setArray(populate(new ExchangeEntryCell[INITIAL_POOL_SIZE]));
    }

    private ExchangeEntryCell[] populate(ExchangeEntryCell[] objects) {
        // Use reverse order so stack will be filled from first to last
        // This also allows early exit, as first non-null element means
        // that we reached the end of new batch.
        for (int i = objects.length - 1; i >= 0; i--) {
            if (objects[i] != null) {
                break;
            }
            objects[i] = new ExchangeEntryCell(i);
            push(objects[i]);
        }

        return objects;
    }

    private static ExchangeEntryCell elementAt(ExchangeEntryCell[] a, int index) {
        return a[index];
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ExchangeEntry<T> lookup(int key) {
        return (ExchangeEntry<T>) elementAt(getArray(), key).entry;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ExchangeEntry<T> acquire(AsyncOperation<T> operation) {
        var cell = pop();

        if (cell == null) {
            synchronized (lock) {
                var es = getArray();
                var len = es.length;

                es = Arrays.copyOf(es, len * 2);
                populate(es);
                setArray(es);

                return acquire(operation);
            }
        }
        return ((ExchangeEntry<T>) cell.entry).operation(operation);
    }

    private <T> void release(ExchangeEntry<T> entry) {
        push(elementAt(getArray(), entry.key()));
    }

    private ExchangeEntryCell pop() {
        ExchangeEntryCell oldHead;
        ExchangeEntryCell newHead;

        do {
            oldHead = head.get();

            if (oldHead == null) {
                return null;
            }

            newHead = oldHead.next;
        } while (!head.compareAndSet(oldHead, newHead));

        return oldHead;
    }

    private void push(ExchangeEntryCell newHead) {
        ExchangeEntryCell oldHead;

        do {
            oldHead = head.get();
            newHead.next = oldHead;
        } while (!head.compareAndSet(oldHead, newHead));
    }

    @Override
    public void clear() {
        synchronized (lock) {
            var array = getArray();

            for (int i = 0; i < array.length; i++) {
                elementAt(array, i).entry.close();
            }
        }
    }

    @Override
    public void completeRequest(CQEntry cqEntry, Proactor proactor) {
        int key = (int) cqEntry.userData();
        var entry = lookup(key);

        entry.processCompletion(cqEntry.res(), cqEntry.flags(), proactor);

        release(entry);
    }
}

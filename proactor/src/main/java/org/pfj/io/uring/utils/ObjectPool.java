/*
 * Copyright (c) 2020 Sergiy Yevtushenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pfj.io.uring.utils;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Resizable pool of mutable objects.
 * Low level utility class for creating pools of reusable (and therefore mutable) objects.
 * Note that this implementation goals were:
 * 1) minimize memory allocation
 * 2) minimal possible overhead
 * As a consequence API is not null-safe, not completely bulletproof and
 * not thread safe. In particular, check for double releases is minimal and
 * does not protect from all possible mistakes.
 * Use with care.
 *
 * @param <T>
 */
public class ObjectPool<T> {
    private Object[] elements;
    private int[] indexes;
    private final Supplier<T> supplier;
    private final Consumer<T> janitor;
    private int firstFree = -1;
    private int nextFree = 0;
    private int count = 0;

    private ObjectPool(final int initialCapacity, final Supplier<T> supplier, final Consumer<T> janitor) {
        elements = new Object[initialCapacity];
        indexes = new int[initialCapacity];
        this.supplier = supplier;
        this.janitor = janitor;
    }

    public static <T> ObjectPool<T> objectPool(final int initialCapacity, final Supplier<T> newElement, final Consumer<T> janitor) {
        return new ObjectPool<>(initialCapacity, newElement, janitor);
    }

    @SuppressWarnings("unchecked")
    public void release(final int key) {
        if (key < 0 || key >= nextFree || key == firstFree) {
            return;
        }

        indexes[key] = firstFree;
        firstFree = key;
        janitor.accept((T) elements[key]);
        count--;
    }

    @SuppressWarnings("unchecked")
    public T get(final int key) {
        if (key < 0 || key >= nextFree || key == firstFree) {
            return null;
        }
        return (T) elements[key];
    }

    public int alloc() {
        // There are some free elements
        if (firstFree >= 0) {
            return allocInFreeChain();
        }

        // No free elements, but still some space
        if (nextFree < elements.length) {
            return allocNew();
        }

        // No free elements and no free space, realloc everything
        indexes = Arrays.copyOf(indexes, indexes.length * 2);
        elements = Arrays.copyOf(elements, elements.length * 2);

        return allocNew();
    }

    private int allocNew() {
        final int index = nextFree++;
        elements[index] = supplier.get();
        indexes[index] = firstFree;
        firstFree = index;
        return allocInFreeChain();
    }

    private int allocInFreeChain() {
        final int result = firstFree;
        firstFree = indexes[result];
        count++;
        return result;
    }

    public int used() {
        return count;
    }

    public int size() {
        return elements.length;
    }
}
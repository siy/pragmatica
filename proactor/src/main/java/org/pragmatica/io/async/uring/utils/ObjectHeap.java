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

package org.pragmatica.io.async.uring.utils;

import java.util.Arrays;

/**
 * Temporary storage for objects which maps instances to integer keys.
 */
public class ObjectHeap<T> {
    private Object[] elements;
    private int[] indexes;
    private int firstFree = -1;
    private int nextFree = 0;
    private int count = 0;

    private ObjectHeap(int initialCapacity) {
        elements = new Object[initialCapacity];
        indexes = new int[initialCapacity];
    }

    public static <T> ObjectHeap<T> objectHeap(int initialCapacity) {
        return new ObjectHeap<>(initialCapacity);
    }

    @SuppressWarnings("unchecked")
    public T releaseUnsafe(int key) {
        indexes[key] = firstFree;
        firstFree = key;
        T result = (T) elements[key];
        elements[key] = null;
        count--;
        return result;
    }

    @SuppressWarnings("unchecked")
    public T elementUnsafe(int key) {
        return (T) elements[key];
    }

    public int allocKey(T value) {
        // There are some free elements
        if (firstFree >= 0) {
            return allocInFreeChain(value);
        }

        // No free elements, but still some space
        if (nextFree < elements.length) {
            return allocNew(value);
        }

        // No free elements and no free space, realloc everything
        indexes = Arrays.copyOf(indexes, indexes.length * 2);
        elements = Arrays.copyOf(elements, elements.length * 2);

        return allocNew(value);
    }

    private int allocNew(T value) {
        int index = nextFree++;
        indexes[index] = firstFree;
        firstFree = index;
        return allocInFreeChain(value);
    }

    private int allocInFreeChain(T value) {
        int result = firstFree;
        elements[result] = value;
        firstFree = indexes[result];
        count++;
        return result;
    }
}
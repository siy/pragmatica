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

package org.pfj.io.async.uring.utils;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Supplier;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ConcurrentObjectPool<T extends Poolable> {
    private volatile T head;
    private final Supplier<T> factory;

    private static final VarHandle HEAD;

    static {
        try {
            final MethodHandles.Lookup l = MethodHandles.lookup();
            HEAD = l.findVarHandle(ConcurrentObjectPool.class, "head", Poolable.class);
        } catch (final ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public ConcurrentObjectPool(final Supplier<T> factory) {
        this.factory = factory;
    }

    public T alloc() {
        final T result = pop();
        return result == null ? factory.get() : (T) result.next(null);
    }

    public void release(final T element) {
        push(element);
    }

    private void push(final T element) {
        T oldHead;

        do {
            oldHead = head;
            element.next(oldHead);
        } while (!HEAD.compareAndSet(this, oldHead, element));
    }

    private T pop() {
        T oldHead;
        T newHead;
        do {
            oldHead = head;
            if (oldHead == null) {
                return null;
            }
            newHead = (T) oldHead.next();
        } while (!HEAD.compareAndSet(this, oldHead, newHead));

        return oldHead;
    }
}

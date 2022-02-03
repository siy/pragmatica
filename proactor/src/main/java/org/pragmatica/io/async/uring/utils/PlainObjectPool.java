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

import org.pragmatica.io.async.uring.CompletionHandler;
import org.pragmatica.io.async.uring.exchange.AbstractExchangeEntry;
import org.pragmatica.lang.Functions.FN1;

@SuppressWarnings({"unchecked", "rawtypes"})
public class PlainObjectPool<T extends AbstractExchangeEntry> {
    private T head;
    private final FN1<T, PlainObjectPool<T>> factory;
    private final ObjectHeap<CompletionHandler> registry;

    private PlainObjectPool(final FN1<T, PlainObjectPool<T>> factory,
                            final ObjectHeap<CompletionHandler> registry) {
        this.factory = factory;
        this.registry = registry;
    }

    public static <T extends AbstractExchangeEntry> PlainObjectPool<T> objectPool(final FN1<T, PlainObjectPool<T>> factory,
                                                                                  final ObjectHeap<CompletionHandler> registry) {
        return new PlainObjectPool<>(factory, registry);
    }

    public T alloc() {
        var result = head;

        if (head == null) {
            return (T) factory.apply(this).register(registry);
        }

        head = (T) head.next;
        return result;
    }

    public void release(final T element) {
        element.next = head;
        head = element;
    }

    public void clear() {
        while (head != null) {
            var element = head;
            head = (T) element.next;
            element.next = null;
            registry.releaseUnsafe(element.key());
        }
    }
}

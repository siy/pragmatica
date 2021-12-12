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

package org.pfj.io.async.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class StackingCollector<T> {
    private volatile Node<T> head;

    private static final VarHandle HEAD;

    static {
        try {
            final MethodHandles.Lookup l = MethodHandles.lookup();
            HEAD = l.findVarHandle(StackingCollector.class, "head", Node.class);
        } catch (final ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private StackingCollector() {
    }

    public static <T> StackingCollector<T> stackingCollector() {
        return new StackingCollector<>();
    }

    public void push(final T action) {
        final var newHead = new Node<>(action);
        Node<T> oldHead;

        do {
            oldHead = head;
            newHead.nextNode = oldHead;
        } while (!HEAD.compareAndSet(this, oldHead, newHead));
    }

    public Node<T> swapHead() {
        Node<T> head;

        do {
            head = this.head;
        } while (!HEAD.compareAndSet(this, head, null));

        Node<T> current = head;
        Node<T> prev = null;
        Node<T> next;

        while (current != null) {
            next = current.nextNode;
            current.nextNode = prev;
            prev = current;
            current = next;
        }

        return prev;
    }

    public static final class Node<T> {
        public T element;
        public Node<T> nextNode;

        public Node(final T element) {
            this.element = element;
        }
    }
}

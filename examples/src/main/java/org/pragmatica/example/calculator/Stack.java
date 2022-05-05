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

package org.pragmatica.example.calculator;

import org.pragmatica.lang.Option;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.pragmatica.lang.Option.option;

public class Stack<E> {
    private final Deque<E> stack = new ArrayDeque<>();

    Option<E> pop() {
        return option(stack.pollFirst());
    }

    Stack<E> push(E value) {
        stack.addFirst(value);
        return this;
    }

    public int size() {
        return stack.size();
    }
}

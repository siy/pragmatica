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

package org.pragmatica.lang;

import java.util.function.Supplier;

public interface Suppliers {
    static<T> Supplier<T> lazy(Supplier<T> sourceFactory) {
        return new Supplier<T>() {
            private final Supplier<T> initializer = this::init;
            private Supplier<T> factory = sourceFactory;
            private volatile Supplier<T> delegate = initializer;

            private synchronized T init() {
                if (delegate != initializer) {
                    return delegate.get();
                }

                var value = factory.get();
                delegate = () -> value;
                factory = null;
                return value;
            }

            @Override
            public T get() {
                return delegate.get();
            }
        };
    }
}

/*
 *  Copyright (c) 2017-2022 Sergiy Yevtushenko.
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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Simple implementation of type token which allows capturing full generic type.
 * <br />
 * In order to use this class, one should create anonymous instance of it with required
 * type:
 * <pre> {@code
 *  new TypeToken<Map<Key, List<Values>>() {}
 * }</pre>
 *
 * Then this instance can be used to retrieve complete generic type of the created instance.
 * Note that this implementation is rudimentary and does not provide any extras, but it's good
 * fit to purposes of capturing parameter type.
 *
 * See http://gafter.blogspot.com/2006/12/super-type-tokens.html for more details.
 */
public abstract class TypeToken<T> {
    public Result<Type> type() {
        if (getClass().getGenericSuperclass() instanceof ParameterizedType parameterizedType) {
            return Result.success(parameterizedType.getActualTypeArguments()[0]);
        }

        return Causes.cause("Unable to retrieve type").result();
    }
}

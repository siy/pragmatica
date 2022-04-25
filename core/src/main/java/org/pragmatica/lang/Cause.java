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

/**
 * Basic interface for failure cause types.
 */
public interface Cause {
    /**
     * Message associated with the failure.
     */
    String message();

    /**
     * The original cause (if any) of the error.
     */
    default Option<Cause> source() {
        return Option.empty();
    }

    /**
     * Represent cause as a failure {@link Result} instance.
     *
     * @return cause converted into {@link Result} with necessary type.
     */
    default <T> Result<T> result() {
        return Result.failure(this);
    }
}

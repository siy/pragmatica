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

package org.pragmatica.io.async.uring;

import org.pragmatica.io.async.Proactor;

/**
 * Low level completion handler
 */
public interface CompletionHandler {
    /**
     * This method is called when request is completed.
     *
     * @param result   Result value (actual meaning depends on the system call)
     * @param flags    Additional flags
     * @param proactor The instance of {@link Proactor} which can be used during request handling.
     */
    void accept(int result, int flags, final Proactor proactor);
}

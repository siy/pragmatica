/*
 *  Copyright (c) 2023 Sergiy Yevtushenko.
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

package org.pragmatica.lang.io;

import org.pragmatica.lang.Result;

public enum CoreError implements Result.Cause {
    CANCELLED("Operation cancelled"),
    TIMEOUT("Operation timed out"),

    FAULT("Operation failed"),

    ;

    private final String message;

    CoreError(String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}

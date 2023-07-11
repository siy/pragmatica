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
 *
 */

package org.pragmatica.io.async.file.stat;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * File status timestamps.
 */
public record StatTimestamp(long seconds, int nanos) {
    public static StatTimestamp timestamp(final long seconds, final int nanos) {
        return new StatTimestamp(seconds, nanos);
    }

    public LocalDateTime localDateTime() {
        return LocalDateTime.ofEpochSecond(seconds, nanos, ZoneOffset.UTC);
    }

    @Override
    public String toString() {
        return localDateTime().toString();
    }
}

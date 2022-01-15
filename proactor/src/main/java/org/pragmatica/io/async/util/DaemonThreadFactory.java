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

package org.pragmatica.io.async.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of {@link ThreadFactory} which creates threads in <code>daemon</code> mode, i.e. threads which while running do not prevent
 * application to stop.
 */
public record DaemonThreadFactory(AtomicInteger counter, String pattern) implements ThreadFactory {
    public static DaemonThreadFactory threadFactory(String pattern) {
        return new DaemonThreadFactory(new AtomicInteger(), pattern);
    }

    @Override
    public Thread newThread(Runnable r) {
        var result = new Thread(r);
        result.setName(String.format(pattern, counter.getAndIncrement()));
        result.setDaemon(true);
        return result;
    }
}

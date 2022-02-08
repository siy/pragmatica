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

package org.pragmatica.io.async;

import org.pragmatica.lang.Tuple.Tuple2;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.pragmatica.io.async.Timeout.TimeoutImpl.fromNanos;
import static org.pragmatica.lang.Tuple.tuple;

/**
 * Representation of timeout value.
 */
public sealed interface Timeout extends Comparable<Timeout> {
    /**
     * Timeout value represented as number of nanoseconds.
     *
     * @return timeout in nanoseconds
     */
    long nanoseconds();

    /**
     * Timeout value represented as number of microseconds.
     *
     * @return timeout in microseconds
     */
    default long microseconds() {
        return TimeUnit.NANOSECONDS.toMicros(nanoseconds());
    }

    /**
     * Timeout value represented as number of milliseconds.
     *
     * @return timeout in milliseconds
     */
    default long milliseconds() {
        return TimeUnit.NANOSECONDS.toMillis(nanoseconds());
    }

    long NANOS_IN_SECOND = TimeUnit.SECONDS.toNanos(1);

    /**
     * Timeout value represented as number of whole seconds and remaining nanoseconds. This representation is compatible with many use cases, for
     * example with {@link Duration} (see {@link #duration()}) and timeout representation used by the kernel API (see {@link
     * org.pragmatica.io.async.uring.struct.offheap.OffHeapTimeSpec}).
     *
     * @return timeout represented as tuple containing number of seconds and remaining nanoseconds
     */
    default Tuple2<Long, Integer> secondsAndNanos() {
        return tuple(nanoseconds() / NANOS_IN_SECOND, (int) (nanoseconds() % NANOS_IN_SECOND));
    }

    /**
     * Timeout value represented as {@link Duration}.
     *
     * @return timeout as {@link Duration}
     */
    default Duration duration() {
        return secondsAndNanos().map(Duration::ofSeconds);
    }

    @Override
    default int compareTo(Timeout o) {
        return Long.compare(nanoseconds(), o.nanoseconds());
    }

    /**
     * Create instance of timeout builder.
     *
     * @param value initial value passed to builder.
     *
     * @return builder instance
     */
    static TimeoutBuilder timeout(long value) {
        return () -> value;
    }

    final class TimeoutImpl implements Timeout {
        private final long nanoseconds;

        private TimeoutImpl(long nanoseconds) {
            this.nanoseconds = nanoseconds;
        }

        @Override
        public long nanoseconds() {
            return nanoseconds;
        }

        static Timeout fromNanos(long nanoseconds) {
            return new TimeoutImpl(nanoseconds);
        }

        @Override
        public boolean equals(Object o) {
            return (this == o) || (o instanceof TimeoutImpl timeout && nanoseconds == timeout.nanoseconds);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nanoseconds);
        }

        @Override
        public String toString() {
            return "Timeout(" + nanoseconds + "ns)";
        }
    }

    /**
     * Fluent interval conversion builder
     */
    interface TimeoutBuilder {
        long value();

        /**
         * Create {@link Timeout} instance by interpreting value as nanoseconds.
         *
         * @return Created instance
         */
        default Timeout nanos() {
            return fromNanos(value());
        }

        /**
         * Create {@link Timeout} instance by interpreting value as microseconds.
         *
         * @return Created instance
         */
        default Timeout micros() {
            return fromNanos(TimeUnit.MICROSECONDS.toNanos(value()));
        }

        /**
         * Create {@link Timeout} instance by interpreting value as milliseconds.
         *
         * @return Created instance
         */
        default Timeout millis() {
            return fromNanos(TimeUnit.MILLISECONDS.toNanos(value()));
        }

        /**
         * Create {@link Timeout} instance by interpreting value as seconds.
         *
         * @return Created instance
         */
        default Timeout seconds() {
            return fromNanos(TimeUnit.SECONDS.toNanos(value()));
        }

        /**
         * Create {@link Timeout} instance by interpreting value as minutes.
         *
         * @return Created instance
         */
        default Timeout minutes() {
            return fromNanos(TimeUnit.MINUTES.toNanos(value()));
        }

        /**
         * Create {@link Timeout} instance by interpreting value as hours.
         *
         * @return Created instance
         */
        default Timeout hours() {
            return fromNanos(TimeUnit.HOURS.toNanos(value()));
        }

        /**
         * Create {@link Timeout} instance by interpreting value as days.
         *
         * @return Created instance
         */
        default Timeout days() {
            return fromNanos(TimeUnit.DAYS.toNanos(value()));
        }
    }
}

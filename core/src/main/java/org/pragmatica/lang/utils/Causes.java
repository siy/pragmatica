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

package org.pragmatica.lang.utils;

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.function.Predicate;

import static org.pragmatica.lang.Option.none;
import static org.pragmatica.lang.Option.some;

/**
 * Frequently used variants of {@link Result.Cause}.
 */
public final class Causes {
    public static final Result.Cause IRRELEVANT = new SimpleCause("irrelevant", none());

    private Causes() {
    }

    /**
     * Simplest possible variant of {@link Result.Cause} which contains only message describing the cause
     */
    record SimpleCause(String message, Option<Result.Cause> source) implements Result.Cause {
    }

    /**
     * Construct a simple cause with a given message.
     *
     * @param message message describing the cause
     *
     * @return created instance
     */
    public static Result.Cause cause(String message) {
        return new SimpleCause(message, none());
    }

    public static Result.Cause cause(String message, Result.Cause source) {
        return new SimpleCause(message, some(source));
    }

    /**
     * Construct a simple cause from provided {@link Throwable}.
     *
     * @param throwable the instance of {@link Throwable} to extract stack trace and message from
     *
     * @return created instance
     */
    public static Result.Cause fromThrowable(Throwable throwable) {
        var sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));

        return cause(sw.toString());
    }

    /**
     * Create a mapper which will map a value into a formatted message. Main use case for this function - creation of mappers for {@link
     * Result#filter(Fn1, Predicate)}:
     * <blockquote><pre>
     * filter(Causes.with1("Value {0} is below threshold"), value -> value > 321)
     * </pre></blockquote>
     *
     * @param template the message template prepared for {@link MessageFormat}
     *
     * @return created mapping function
     */
    public static <T> Fn1<Result.Cause, T> with1(String template) {
        return (T input) -> cause(MessageFormat.format(template, input));
    }
}

package org.pfj.lang;

import org.pfj.lang.Functions.FN1;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.function.Predicate;

/**
 * Common variants of {@link Cause}.
 */
//TODO: add more variants
public final class Causes {
    private Causes() {
    }

    /**
     * Simplest possible variant of {@link Cause} which contains only message describing the cause
     */
    record SimpleCause(String message) implements Cause {
    }

    /**
     * Construct a simple cause with a given message.
     *
     * @param message message describing the cause
     *
     * @return created instance
     */
    public static Cause cause(String message) {
        return new SimpleCause(message);
    }

    /**
     * Construct a simple cause from provided {@link Throwable}.
     *
     * @param throwable the instance of {@link Throwable} to extract stack trace and message from
     *
     * @return created instance
     */
    public static Cause fromThrowable(Throwable throwable) {
        var sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));

        return new SimpleCause(sw.toString());
    }

    /**
     * Create a mapper which will map a value into a formatted message.
     * Main use case for this function - creation of mappers for {@link Result#filter(FN1, Predicate)}:
     * <blockquote><pre>
     * filter(Causes.with1("Value {0} is below threshold"), value -> value > 321)
     * </pre></blockquote>
     *
     * @param template the message template prepared for {@link MessageFormat}
     *
     * @return created mapping function
     */
    public static <T> FN1<Cause, T> with1(String template) {
        return (T input) -> cause(MessageFormat.format(template, input));
    }
}

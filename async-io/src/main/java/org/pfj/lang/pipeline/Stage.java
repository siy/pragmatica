package org.pfj.lang.pipeline;

import org.pfj.lang.Functions.FN1;

import java.util.function.Consumer;
import java.util.function.Predicate;

public interface Stage<T> {
    Stage<T> push(T value);

    Stage<T> onValue(Consumer<T> action);

    Stage<T> filter(Predicate<T> predicate);

    <R> Stage<R> map(FN1<R, ? super T> mapper);

    <R> Stage<R> mapIf(Predicate<T> condition, FN1<R, ? super T> trueMapper, FN1<R, ? super T> falseMapper);

    static <T> Stage<T> stage() {
        return new StageImpl<>();
    }
}

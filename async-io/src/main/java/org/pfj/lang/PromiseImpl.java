package org.pfj.lang;

import org.pfj.lang.Functions.FN1;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Consumer;

public class PromiseImpl<T> implements Promise<T> {
    private volatile Result<T> value;
    private volatile CompletionAction<T> head;

    private static final VarHandle HEAD;
    private static final VarHandle VALUE;

    private static class CompletionAction<T> {
        private volatile CompletionAction<T> next;
        private final Consumer<Result<T>> action;

        private CompletionAction(Consumer<Result<T>> action) {
            this.action = action;
        }
    }

    static {
        try {
            final MethodHandles.Lookup l = MethodHandles.lookup();
            HEAD = l.findVarHandle(PromiseImpl.class, "head", CompletionAction.class);
            VALUE = l.findVarHandle(PromiseImpl.class, "value", Result.class);
        } catch (final ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    PromiseImpl(Result<T> value) {
        this.value = value;
    }

    @Override
    public <U> Promise<U> map(FN1<U, ? super T> mapper) {
        if (value != null) {
            return new PromiseImpl<>(value.map(mapper));
        }

        var result = new PromiseImpl<U>(null);

        push(new CompletionAction<T>(value -> result.resolve(value.map(mapper))));

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U> Promise<U> flatMap(FN1<Promise<U>, ? super T> mapper) {
        if (value != null) {
            return value.fold(f -> new PromiseImpl<>((Result<U>) value), mapper);
        }

        var result = new PromiseImpl<U>(null);

        push(new CompletionAction<T>(
            value -> value.fold(f -> new PromiseImpl<>((Result<U>) value), mapper)
                .thenDo(result::resolve)));

        return result;
    }

    @Override
    public Promise<T> thenDo(Consumer<Result<T>> action) {
        return push(new CompletionAction<T>(action));
    }

    @Override
    public Promise<T> resolve(Result<T> value) {
        if (VALUE.compareAndSet(this, null, value)) {
            runActions(value);
        }

        return this;
    }

    private void runActions(Result<T> value) {
        CompletionAction<T> head;

        while ((head = swapHead()) != null) {
            while (head != null) {
                head.action.accept(value);
                head = head.next;
            }
        }
    }

    private PromiseImpl<T> push(CompletionAction<T> newHead) {
        CompletionAction<T> oldHead;

        do {
            oldHead = head;
            newHead.next = oldHead;
        } while (!HEAD.compareAndSet(this, oldHead, newHead));

        return this;
    }

    private CompletionAction<T> swapHead() {
        CompletionAction<T> head;

        do {
            head = this.head;
        } while (!HEAD.compareAndSet(this, head, null));

        CompletionAction<T> current = head;
        CompletionAction<T> prev = null;
        CompletionAction<T> next;

        //Reverse list
        while (current != null) {
            next = current.next;
            current.next = prev;
            prev = current;
            current = next;
        }

        return prev;
    }
}

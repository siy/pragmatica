package org.pragmatica.io.async.uring.utils;

import org.pragmatica.io.async.uring.exchange.ExchangeEntry;

import java.util.concurrent.LinkedTransferQueue;
import java.util.function.Supplier;

public class ExchangeEntryPool<T extends ExchangeEntry<T>> {
//    private volatile T head;
    private final Supplier<T> factory;
    private final ExchangeEntryRegistry registry;

    private final LinkedTransferQueue<T> queue = new LinkedTransferQueue<>();

//    private static final VarHandle HEAD;
//
//    static {
//        try {
//            final MethodHandles.Lookup l = MethodHandles.lookup();
//            HEAD = l.findVarHandle(ExchangeEntryPool.class, "head", ExchangeEntry.class);
//        } catch (final ReflectiveOperationException e) {
//            throw new ExceptionInInitializerError(e);
//        }
//    }

    private ExchangeEntryPool(final Supplier<T> factory, ExchangeEntryRegistry registry) {
        this.factory = factory;
        this.registry = registry;
    }

    public static <T extends ExchangeEntry<T>> ExchangeEntryPool<T> exchangeEntryPool(final Supplier<T> factory,
                                                                                      final ExchangeEntryRegistry registry) {
        return new ExchangeEntryPool<>(factory, registry);
    }

    public T alloc() {
        final T result = pop();

        if (result != null) {
            return result.ensureUnused("Used entry is allocated again").next(null);
        }

        return registry.register(factory.get().ensureUnused("New entry pretends to be used"), this);
    }

    public void release(final T element) {
        push(element);
    }

    public void clear() {
        T element;

        while ((element = pop()) != null) {
            element.close();
        }
    }

    private void push(final T element) {
        element.ensureUnused("Used entry is released back to pool");

        queue.put(element);

//        T oldHead;
//
//        do {
//            oldHead = head;
//            element.next(oldHead);
//        } while (!HEAD.compareAndSet(this, oldHead, element));
    }

    private T pop() {
        return queue.poll();
//        T oldHead;
//        T newHead;
//        do {
//            oldHead = head;
//            if (oldHead == null) {
//                return null;
//            }
//            newHead = (T) oldHead.next();
//        } while (!HEAD.compareAndSet(this, oldHead, newHead));
//
//        return oldHead;
    }
}

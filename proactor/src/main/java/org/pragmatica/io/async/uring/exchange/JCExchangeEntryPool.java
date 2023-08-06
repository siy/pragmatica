package org.pragmatica.io.async.uring.exchange;

import org.jctools.queues.MpmcArrayQueue;
import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.uring.struct.raw.CQEntry;

import static org.pragmatica.io.async.uring.exchange.ExchangeEntry.exchangeEntry;

public class JCExchangeEntryPool implements ExchangeEntryPool {
    private static final int INITIAL_POOL_SIZE = 2048;
    private final ExchangeEntry<?>[] array = new ExchangeEntry<?>[INITIAL_POOL_SIZE];
    private final MpmcArrayQueue<ExchangeEntry<?>> queue = new MpmcArrayQueue<>(INITIAL_POOL_SIZE);

    public JCExchangeEntryPool() {
        for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
            var entry = exchangeEntry(i);
            array[i] = entry;
            queue.offer(entry);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ExchangeEntry<T> acquire(AsyncOperation<T> operation) {
        return ((ExchangeEntry<T>) queue.poll()).operation(operation);
    }

    @Override
    public int size() {
        return INITIAL_POOL_SIZE - queue.size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ExchangeEntry<T> lookup(int key) {
        return (ExchangeEntry<T>) array[key];
    }

    @Override
    public void clear() {
        queue.clear();

        for (ExchangeEntry<?> exchangeEntry : array) {
            exchangeEntry.close();
        }
    }

    @Override
    public void completeRequest(CQEntry cqEntry, Proactor proactor) {
        int key = (int) cqEntry.userData();
        var entry = lookup(key);

        entry.processCompletion(cqEntry, proactor);

        queue.offer(entry);
    }
}

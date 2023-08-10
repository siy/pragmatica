package org.pragmatica.io.async.uring.exchange;

import org.pragmatica.io.async.Proactor;

public interface ExchangeEntryPool {
    <T> ExchangeEntry<T> acquire(AsyncOperation<T> operation);

    <T> ExchangeEntry<T> lookup(int key);

    void clear();

    void completeRequest(long key, int res, int flags, Proactor proactor);

    static ExchangeEntryPool exchangeEntryPool() {
        return new HybridExchangeEntryPool();
    }
}

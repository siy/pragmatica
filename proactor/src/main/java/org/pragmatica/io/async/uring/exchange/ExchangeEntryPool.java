package org.pragmatica.io.async.uring.exchange;

import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.uring.struct.raw.CQEntry;

public interface ExchangeEntryPool {
    <T> ExchangeEntry<T> acquire(AsyncOperation<T> operation);

    <T> ExchangeEntry<T> lookup(int key);

    void clear();

    void completeRequest(CQEntry cqEntry, Proactor proactor);

    static ExchangeEntryPool arrayPool() {
        return new ArrayExchangeEntryPool();
    }

    static ExchangeEntryPool hybridPool() {
        return new HybridExchangeEntryPool();
    }
}

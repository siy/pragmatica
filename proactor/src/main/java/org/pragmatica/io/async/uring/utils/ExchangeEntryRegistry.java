package org.pragmatica.io.async.uring.utils;

import org.pragmatica.io.async.uring.exchange.ExchangeEntry;

import java.util.ArrayList;
import java.util.List;

public class ExchangeEntryRegistry {
    private final List<ExchangeEntry<?>> entries = new ArrayList<>();

    private ExchangeEntryRegistry() {
    }

    public static ExchangeEntryRegistry exchangeEntryRegistry() {
        return new ExchangeEntryRegistry();
    }

    public <T extends ExchangeEntry<T>> T register(final T entry, final ExchangeEntryPool<T> pool) {
        synchronized (entries) {
            entry.key(entries.size(), pool);
            entries.add(entry);
            return entry;
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends ExchangeEntry<T>> T lookup(final int key) {
        return (T) entries.get(key);
    }
}

package org.pfj.lang;

public interface Resolvable<T> {
    void resolve(Result<T> value);
}

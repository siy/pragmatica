package org.pfj.lang;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PromiseTest {

    @Test
    void promiseCanBeResolved() {
        var promise = Promise.<Integer>promise();

        var ref = new AtomicInteger();

        promise.resolve(Result.success(1));
    }
}
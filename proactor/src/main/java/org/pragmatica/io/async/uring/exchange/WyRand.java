package org.pragmatica.io.async.uring.exchange;

public final class WyRand {
    private long seed = System.nanoTime() ^ System.currentTimeMillis();

    private WyRand() {
    }

    public long next() {
        seed += 0xa0761d6478bd642fL;
        long see1 = seed ^ 0xe7037ed1a0b428dbL;
        see1 *= (see1 >> 32) | (see1 << 32);
        return (seed * ((seed >> 32) | (seed << 32))) ^ ((see1 >> 32) | (see1 << 32));
    }

    public static WyRand wyRand() {
        return new WyRand();
    }
}

package org.pragmatica.io.async.uring.exchange;

/*
 * The following two comments are quoted from http://prng.di.unimi.it/xoshiro256plusplus.c
 */

/*
 * To the extent possible under law, the author has dedicated all copyright
 * and related and neighboring rights to this software to the public domain
 * worldwide. This software is distributed without any warranty.
 * <p>
 * See http://creativecommons.org/publicdomain/zero/1.0/.
 */

/*
 * This is xoshiro256++ 1.0, one of our all-purpose, rock-solid generators.
 * It has excellent (sub-ns) speed, a state (256 bits) that is large
 * enough for any parallel application, and it passes all tests we are
 * aware of.
 *
 * For generating just floating-point numbers, xoshiro256+ is even faster.
 *
 * The state must be seeded so that it is not everywhere zero. If you have
 * a 64-bit seed, we suggest to seed a splitmix64 generator and use its
 * output to fill s.
 */
public class Xoshiro256PlusPlus {
    private static final long GOLDEN_RATIO_64 = 0x9e3779b97f4a7c15L;
    private static final long SILVER_RATIO_64 = 0x6A09E667F3BCC909L;

    private static long seed() {
        return (mixStafford13(System.currentTimeMillis()) ^
                mixStafford13(System.nanoTime()));
    }

    private static long mixStafford13(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    private long x0, x1, x2, x3;

    private Xoshiro256PlusPlus(long x0, long x1, long x2, long x3) {
        this.x0 = x0;
        this.x1 = x1;
        this.x2 = x2;
        this.x3 = x3;
    }

    public static Xoshiro256PlusPlus xoshiro256PlusPlus() {
        long seed = seed() + GOLDEN_RATIO_64;

        return new Xoshiro256PlusPlus(mixStafford13(seed ^= SILVER_RATIO_64),
                                      mixStafford13(seed += GOLDEN_RATIO_64),
                                      mixStafford13(seed += GOLDEN_RATIO_64),
                                      mixStafford13(seed + GOLDEN_RATIO_64));
    }

    public long next() {
        // Compute the result based on current state information
        // (this allows the computation to be overlapped with state update).
        final long result = Long.rotateLeft(x0 + x3, 23) + x0;  // "plusplus" scrambler

        long q0 = x0, q1 = x1, q2 = x2, q3 = x3;
        {   // xoshiro256 1.0
            long t = q1 << 17;
            q2 ^= q0;
            q3 ^= q1;
            q1 ^= q2;
            q0 ^= q3;
            q2 ^= t;
            q3 = Long.rotateLeft(q3, 45);
        }
        x0 = q0; x1 = q1; x2 = q2; x3 = q3;
        return result;
    }
}

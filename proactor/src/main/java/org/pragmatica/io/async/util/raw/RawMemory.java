/*
 *  Copyright (c) 2022 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.pragmatica.io.async.util.raw;

import jdk.internal.misc.Unsafe;

/**
 * Simple raw memory access API
 */
public final class RawMemory {
    private static final Unsafe instance = Unsafe.getUnsafe();

    private RawMemory() {}

    // Access with enforced network (big-endian) byte order
    public static short getShortInNetOrder(long address) {
        return instance.getShortUnaligned(null, address, true);
    }

    public static void putShortInNetOrder(long address, short value) {
        instance.putShortUnaligned(null, address, value, true);
    }

    public static int getIntInNetOrder(long address) {
        return instance.getIntUnaligned(null, address, true);
    }

    public static void putIntInNetOrder(long address, int value) {
        instance.putIntUnaligned(null, address, value, true);
    }

    // Raw access of different size
    public static long getLong(long address) {
        return instance.getLong(address);
    }

    public static int getInt(long address) {
        return instance.getInt(address);
    }

    public static short getShort(long address) {
        return instance.getShort(address);
    }

    public static byte getByte(long address) {
        return instance.getByte(address);
    }

    public static byte[] getByteArray(long address, int length) {
        var output = new byte[length];
        instance.copyMemory(null, address, output, Unsafe.ARRAY_BYTE_BASE_OFFSET, length);
        return output;
    }

    public static void putLong(long address, long value) {
        instance.putLong(address, value);
    }

    public static void putInt(long address, int value) {
        instance.putInt(address, value);
    }

    public static void putShort(long address, short value) {
        instance.putShort(address, value);
    }

    public static void putByte(long address, byte value) {
        instance.putByte(address, value);
    }

    public static void putByteArray(long address, byte[] input) {
        putByteArray(address, input, input.length);
    }

    public static void putByteArray(long address, byte[] input, int maxLen) {
        var len = Math.min(maxLen, input.length);
        instance.copyMemory(input, Unsafe.ARRAY_BYTE_BASE_OFFSET, null, address, len);
    }

    // Memory management
    public static long allocate(long amount) {
        return instance.allocateMemory(amount);
    }

    public static long reallocate(long address, long newAmount) {
        return instance.reallocateMemory(address, newAmount);
    }

    public static void dispose(long address) {
        instance.freeMemory(address);
    }

    public static void clear(long address, long size) {
        instance.setMemory(address, size, (byte) 0);
    }
}

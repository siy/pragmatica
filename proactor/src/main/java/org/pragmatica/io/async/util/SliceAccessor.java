/*
 *  Copyright (c) 2020-2022 Sergiy Yevtushenko.
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

package org.pragmatica.io.async.util;

import org.pragmatica.io.async.util.raw.RawMemory;

/**
 * Sequential (with position) reader/writer of {@link OffHeapSlice}.
 */
public final class SliceAccessor {
    private static final byte[] EMPTY_ARRAY = {};
    private final OffHeapSlice slice;
    private int position;

    private SliceAccessor(OffHeapSlice slice) {
        this.slice = slice;
        this.position = 0;
    }

    public static SliceAccessor forSlice(OffHeapSlice slice) {
        return new SliceAccessor(slice);
    }

    private long addressForPositionAdjustedBy(int width) {
        var baseAddress = validateAddressFor(width);
        position += width;

        return baseAddress;
    }

    private long validateAddressFor(int width) {
        if (position + width > slice.size()) {
            throw new IllegalStateException("Attempt to read past the end of slice. Size: "
                                            + slice.size() + ", Position: " + position + ", Width: " + width);
        }
        return slice.address() + position;
    }

    public short getShortInNetOrder() {
        return RawMemory.getShortInNetOrder(addressForPositionAdjustedBy(Short.BYTES));
    }

    public int getUnsignedShortInNetOrder() {
        return (getShortInNetOrder() & 0xFFFF);
    }

    public int getIntInNetOrder() {
        return RawMemory.getIntInNetOrder(addressForPositionAdjustedBy(Integer.BYTES));
    }

    public long getLongInNetOrder() {
        return RawMemory.getLongInNetOrder(addressForPositionAdjustedBy(Long.BYTES));
    }

    public long getLong() {
        return RawMemory.getLong(addressForPositionAdjustedBy(Long.BYTES));
    }

    public int getInt() {
        return RawMemory.getInt(addressForPositionAdjustedBy(Integer.BYTES));
    }

    public short getShort() {
        return RawMemory.getShort(addressForPositionAdjustedBy(Short.BYTES));
    }

    public byte getByte() {
        return RawMemory.getByte(addressForPositionAdjustedBy(Byte.BYTES));
    }

    public short getUnsignedByte() {
        return (short)(getByte() & 0xFF);
    }

    public int availableForRead() {
        return slice.used() - position;
    }

    public byte peekByte() {
        return RawMemory.getByte(validateAddressFor(Byte.BYTES));
    }

    public byte[] getBytes(int length) {
        return RawMemory.getByteArray(addressForPositionAdjustedBy(length), length);
    }

    public byte[] getRemainingBytes() {
        var length = availableForRead();
        if (length <= 0) {
            return EMPTY_ARRAY;
        }
        return RawMemory.getByteArray(addressForPositionAdjustedBy(length), length);
    }

    public SliceAccessor putShortInNetOrder(short value) {
        RawMemory.putShortInNetOrder(addressForPositionAdjustedBy(Short.BYTES), value);
        return this;
    }

    public SliceAccessor putIntInNetOrder(int value) {
        RawMemory.putIntInNetOrder(addressForPositionAdjustedBy(Integer.BYTES), value);
        return this;
    }

    public SliceAccessor putLongInNetOrder(long value) {
        RawMemory.putLongInNetOrder(addressForPositionAdjustedBy(Long.BYTES), value);
        return this;
    }

    public SliceAccessor putLong(long value) {
        RawMemory.putLong(addressForPositionAdjustedBy(Long.BYTES), value);
        return this;
    }

    public SliceAccessor putInt(int value) {
        RawMemory.putInt(addressForPositionAdjustedBy(Integer.BYTES), value);
        return this;
    }

    public SliceAccessor putShort(short value) {
        RawMemory.putShort(addressForPositionAdjustedBy(Short.BYTES), value);
        return this;
    }

    public SliceAccessor putByte(byte value) {
        RawMemory.putByte(addressForPositionAdjustedBy(Byte.BYTES), value);
        return this;
    }

    public SliceAccessor putBytes(byte[] input) {
        RawMemory.putByteArray(addressForPositionAdjustedBy(input.length), input);
        return this;
    }

    public SliceAccessor clear() {
        slice.clear();
        position = 0;
        return this;
    }

    public SliceAccessor position(int position) {
        this.position = position;
        return this;
    }

    public int position() {
        return position;
    }

    public SliceAccessor updateSlice() {
        slice.used(position);
        return this;
    }

    public boolean availableByte() {
        return availableForRead() >= Byte.BYTES;
    }

    public boolean availableShort() {
        return availableForRead() >= Short.BYTES;
    }

    public boolean availableInt() {
        return availableForRead() >= Integer.BYTES;
    }

    public boolean availableLong() {
        return availableForRead() >= Integer.BYTES;
    }

    public boolean availableBytes(int len) {
        return availableForRead() >= len;
    }

    public int used() {
        return slice.used();
    }
}

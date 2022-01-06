/*
 * Copyright (c) 2020 Sergiy Yevtushenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pfj.io.async.uring.struct;

import org.pfj.io.async.util.raw.RawMemory;
import org.pfj.io.async.util.raw.RawProperty;

public abstract class AbstractRawStructure<T extends RawStructure<T>> implements RawStructure<T> {
    private long address;
    private final int size;

    protected AbstractRawStructure(final long address, final int size) {
        this.address = address;
        this.size = size;
    }

    @Override
    public long address() {
        return address;
    }

    protected void address(final long address) {
        this.address = address;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T clear() {
        RawMemory.clear(address(), size());
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    protected T putByte(final RawProperty property, final byte value) {
        RawMemory.putByte(address + property.offset(), value);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    protected T putShort(final RawProperty property, final short value) {
        RawMemory.putShort(address + property.offset(), value);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    protected T putInt(final RawProperty property, final int value) {
        RawMemory.putInt(address + property.offset(), value);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    protected T putLong(final RawProperty property, final long value) {
        RawMemory.putLong(address + property.offset(), value);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    protected T putShortInNetOrder(final RawProperty property, final short value) {
        RawMemory.putShortInNetOrder(address + property.offset(), value);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    protected T putIntInNetOrder(final RawProperty property, final int value) {
        RawMemory.putIntInNetOrder(address + property.offset(), value);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    protected T putBytes(final RawProperty property, final byte[] value) {
        RawMemory.putByteArray(address + property.offset(), value);
        return (T) this;
    }

    protected byte getByte(final RawProperty property) {
        return RawMemory.getByte(address + property.offset());
    }

    protected short getShort(final RawProperty property) {
        return RawMemory.getShort(address + property.offset());
    }

    protected int getInt(final RawProperty property) {
        return RawMemory.getInt(address + property.offset());
    }

    protected long getLong(final RawProperty property) {
        return RawMemory.getLong(address + property.offset());
    }

    protected short getShortInNetOrder(final RawProperty property) {
        return RawMemory.getShortInNetOrder(address + property.offset());
    }

    protected int getIntInNetOrder(final RawProperty property) {
        return RawMemory.getIntInNetOrder(address + property.offset());
    }

    protected byte[] getBytes(final RawProperty property) {
        return RawMemory.getByteArray(address + property.offset(), property.size());
    }
}

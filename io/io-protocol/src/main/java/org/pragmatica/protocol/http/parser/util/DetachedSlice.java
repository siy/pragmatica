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

package org.pragmatica.protocol.http.parser.util;

import java.nio.charset.Charset;

public final class DetachedSlice {
    private int start;
    private int end;

    public DetachedSlice() {
        this(0, 0);
    }

    public DetachedSlice(DetachedSlice other) {
        this(other.start, other.end);
    }

    public DetachedSlice(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public String text(byte[] data, Charset charset) {
        return new String(data, start, len(), charset);
    }

    public int len() {
        return end - start;
    }

    public DetachedSlice len(int len) {
        end = start + len;
        return this;
    }


    public int start() {
        return start;
    }

    public DetachedSlice start(int start) {
        this.start = start;
        return this;
    }

    public int end() {
        return end;
    }

    public DetachedSlice end(int end) {
        this.end = end;
        return this;
    }
}

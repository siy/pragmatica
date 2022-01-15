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

package org.pragmatica.io.pipeline;

import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class StageTest {
    @Test
    void stageCanProcessData() {
        var ref1 = new AtomicReference<Integer>();
        var ref2 = new AtomicReference<String>();
        var ref3 = new AtomicReference<Long>();
        var stage = Stage.<Integer>stage();

        stage
            .onValue(ref1::set)
            .map(Objects::toString)
            .onValue(ref2::set)
            .map(Long::parseLong)
            .onValue(ref3::set);

        assertNull(ref1.get());
        assertNull(ref2.get());
        assertNull(ref3.get());

        stage.push(1);

        assertEquals(1, ref1.get());
        assertEquals("1", ref2.get());
        assertEquals(1L, ref3.get());

        stage.push(2);

        assertEquals(2, ref1.get());
        assertEquals("2", ref2.get());
        assertEquals(2L, ref3.get());
    }

    @Test
    void valuesCanBeFiltered() {
        var ref1 = new AtomicInteger();
        var ref2 = new AtomicInteger();
        var stage = Stage.<Integer>stage();

        stage
            .onValue(ref1::set)
            .filter(value -> value > 2)
            .onValue(ref2::set);

        assertEquals(0, ref1.get());
        assertEquals(0, ref2.get());

        stage.push(1);

        assertEquals(1, ref1.get());
        assertEquals(0, ref2.get());

        stage.push(3);

        assertEquals(3, ref1.get());
        assertEquals(3, ref2.get());

        stage.push(2);

        assertEquals(2, ref1.get());
        assertEquals(3, ref2.get());
    }

    @Test
    void stageCanMapConditionally() {
        var ref1 = new AtomicInteger();
        var ref2 = new AtomicInteger();
        var stage = Stage.<Integer>stage();

        stage
            .onValue(ref1::set)
            .mapIf(value -> value > 2, value -> value/2, value -> value * 2)
            .onValue(ref2::set);

        assertEquals(0, ref1.get());
        assertEquals(0, ref2.get());

        stage.push(1);

        assertEquals(1, ref1.get());
        assertEquals(2, ref2.get());

        stage.push(3);

        assertEquals(3, ref1.get());
        assertEquals(1, ref2.get());
    }
}
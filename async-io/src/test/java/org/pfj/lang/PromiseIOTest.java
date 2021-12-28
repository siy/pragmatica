/*
 *  Copyright (c) 2021 Sergiy Yevtushenko.
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

package org.pfj.lang;

import org.junit.jupiter.api.Test;
import org.pfj.io.async.Proactor;
import org.pfj.io.async.Timeout;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.pfj.io.async.Timeout.timeout;

public class PromiseIOTest {
    @Test
    void nopCanBeSubmitted() {
        var result = Promise.<Unit>promise((p1, proactor) -> proactor.nop(p1::resolve))
                            .join();

        assertEquals(Unit.unitResult(), result);
    }

    @Test
    void delayCanBeSubmitted() {
        var delay = 100;
        var result = Promise.<Duration>promise((p1, proactor) -> proactor.delay(p1::resolve, timeout(delay).millis()))
                            .join();

        result.onSuccess(duration -> assertTrue(duration.compareTo(Duration.ofMillis(delay)) >= 0))
            .onFailure(cause -> fail(cause.message()));
    }

//    @Test
//    void fileCanBeOpenReadAndClosed() {
//        final var fileName = "target/classes/" + Promise.class.getName().replace('.', '/') + ".class";
//
//        var result = Promise.<Duration>promise((p1, proactor) -> proactor.delay(p1::resolve, timeout(delay).millis()))
//                            .join();
//    }
}

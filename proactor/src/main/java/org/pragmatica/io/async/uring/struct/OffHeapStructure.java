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
 */

package org.pragmatica.io.async.uring.struct;

/**
 * API for classes which are used for exchanging data with JNI side. Note that these classes are allocating and deallocating off-heap memory.
 * Lifecycle of instances of such classes should be carefully tracked to avoid memory leaks.
 */
public interface OffHeapStructure<T extends RawStructure<T>> extends RawStructure<T> {
    void dispose();
}

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

package org.pragmatica.io.async.uring;

/**
 * Register operation opcodes.
 * <p>
 * WARNING: Always keep in sync with io_uring.h
 * <p>
 * Ordinals (by the means of .ordinal() call) of constants below must match actual Linux API codes (as defined in io_uring.h).
 */
public enum RegisterOperation {
    IORING_REGISTER_BUFFERS,
    IORING_UNREGISTER_BUFFERS,
    IORING_REGISTER_FILES,
    IORING_UNREGISTER_FILES,
    IORING_REGISTER_EVENTFD,
    IORING_UNREGISTER_EVENTFD,
    IORING_REGISTER_FILES_UPDATE,
    IORING_REGISTER_EVENTFD_ASYNC,
    IORING_REGISTER_PROBE,
    IORING_REGISTER_PERSONALITY,
    IORING_UNREGISTER_PERSONALITY,
    IORING_REGISTER_RESTRICTIONS,
    IORING_REGISTER_ENABLE_RINGS,
    IORING_REGISTER_FILES2,             /* extended with tagging */
    IORING_REGISTER_FILES_UPDATE2,
    IORING_REGISTER_BUFFERS2,
    IORING_REGISTER_BUFFERS_UPDATE,
    IORING_REGISTER_IOWQ_AFF,           /* set/clear io-wq thread affinities */
    IORING_UNREGISTER_IOWQ_AFF,
    IORING_REGISTER_IOWQ_MAX_WORKERS,   /* set/get max number of io-wq workers */
    IORING_REGISTER_LAST;               /* this goes last */
}

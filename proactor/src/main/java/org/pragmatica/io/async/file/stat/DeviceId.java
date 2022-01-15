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

package org.pragmatica.io.async.file.stat;

/**
 * Container for Unix-like device ID.
 */
public class DeviceId {
    private final int major;
    private final int minor;

    private DeviceId(final int major, final int minor) {
        this.major = major;
        this.minor = minor;
    }

    public static DeviceId deviceId(final int major, final int minor) {
        return new DeviceId(major, minor);
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    @Override
    public String toString() {
        return "DeviceId(" + major + ", " + minor + ")";
    }
}

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

package org.pragmatica.io.async.file;

/**
 * General purpose Linux file descriptor.
 */
public record FileDescriptor(int descriptor, DescriptorType type) {
    public static FileDescriptor file(int fd) {
        return new FileDescriptor(fd, DescriptorType.FILE);
    }

    public static FileDescriptor socket(int fd) {
        return new FileDescriptor(fd, DescriptorType.SOCKET);
    }

    public static FileDescriptor socket6(int fd) {
        return new FileDescriptor(fd, DescriptorType.SOCKET6);
    }

    public boolean isSocket() {
        return type != DescriptorType.FILE;
    }

    public boolean isSocket6() {
        return type == DescriptorType.SOCKET6;
    }

    @Override
    public String toString() {
        return "FileDescriptor(" + descriptor + ", " + type + ")";
    }
}

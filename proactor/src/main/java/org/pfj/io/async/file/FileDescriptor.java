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

package org.pfj.io.async.file;

import java.util.Objects;

/**
 * General purpose Linux file descriptor.
 */
public class FileDescriptor {
    private final int fd;
    private final DescriptorType type;

    private FileDescriptor(final int fd, final DescriptorType type) {
        this.fd = fd;
        this.type = type;
    }

    public boolean isSocket() {
        return type != DescriptorType.FILE;
    }

    public boolean isSocket6() {
        return type == DescriptorType.SOCKET6;
    }

    public int descriptor() {
        return fd;
    }

    public static FileDescriptor file(final int fd) {
        return new FileDescriptor(fd, DescriptorType.FILE);
    }

    public static FileDescriptor socket(final int fd) {
        return new FileDescriptor(fd, DescriptorType.SOCKET);
    }

    public static FileDescriptor socket6(final int fd) {
        return new FileDescriptor(fd, DescriptorType.SOCKET6);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof FileDescriptor other) {
            return fd == other.fd && type == other.type;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fd, type);
    }

    @Override
    public String toString() {
        return "FileDescriptor(" + fd + ", " + type + ")";
    }
}

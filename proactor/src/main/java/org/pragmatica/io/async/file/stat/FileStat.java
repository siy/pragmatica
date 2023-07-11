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

package org.pragmatica.io.async.file.stat;

import org.pragmatica.io.async.file.FilePermission;

import java.util.EnumSet;
import java.util.StringJoiner;

/**
 * Extended Linux file status information.
 */
public record FileStat(
    EnumSet<StatMask> mask,
    int blockSize,
    EnumSet<StatAttribute> attributes,
    int numLinks,
    int ownerUID,
    int ownerGID,
    FileType fileType,
    EnumSet<FilePermission> permissions,
    long inode,
    long size,
    long blocks,
    EnumSet<StatAttribute> attributeMask, // Supported attributes mask
    StatTimestamp accessTime, /* Last access time */
    StatTimestamp creationTime, /* File creation time */
    StatTimestamp attributeChangeTime, /* Last attribute change time */
    StatTimestamp modificationTime, /* Last data modification time */
    DeviceId rDevice,
    DeviceId fsDevice
) {

    public static FileStat fileStat(EnumSet<StatMask> mask, int blockSize, EnumSet<StatAttribute> attributes,
                                    int numLinks, int ownerUID, int ownerGID, FileType fileType, EnumSet<FilePermission> permissions,
                                    long inode, long size, long blocks, EnumSet<StatAttribute> attributeMask,
                                    StatTimestamp accessTime, StatTimestamp creationTime,
                                    StatTimestamp attributeChangeTime, StatTimestamp modificationTime,
                                    DeviceId rDevice, DeviceId fsDevice) {
        return new FileStat(mask, blockSize, attributes, numLinks, ownerUID, ownerGID, fileType, permissions, inode, size, blocks,
                            attributeMask, accessTime, creationTime, attributeChangeTime, modificationTime, rDevice, fsDevice);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "FileStat(", ")")
            .add("mask:" + mask)
            .add("blockSize:" + blockSize)
            .add("attributes:" + attributes)
            .add("numLinks:" + numLinks)
            .add("ownerUID:" + ownerUID)
            .add("ownerGID:" + ownerGID)
            .add("fileType:" + fileType)
            .add("permissions:" + permissions)
            .add("inode:" + inode)
            .add("size:" + size)
            .add("blocks:" + blocks)
            .add("attributeMask:" + attributeMask)
            .add("accessTime:" + accessTime)
            .add("creationTime: " + creationTime.localDateTime())
            .add("attributeChangeTime: " + attributeChangeTime.localDateTime())
            .add("modificationTime: " + modificationTime.localDateTime())
            .add("rDevice: " + rDevice)
            .add("fsDevice: " + fsDevice)
            .toString();
    }
}

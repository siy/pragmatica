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

package org.pfj.io.async.file.stat;

import org.pfj.io.async.file.FilePermission;

import java.util.EnumSet;
import java.util.StringJoiner;

/**
 * Extended Linux file status information.
 */
public class FileStat {
    private final EnumSet<StatMask> mask;
    private final int blockSize;
    private final EnumSet<StatAttribute> attributes;
    private final int numLinks;
    private final int ownerUID;
    private final int ownerGID;
    private final FileType fileType;
    private final EnumSet<FilePermission> permissions;
    private final long inode;
    private final long size;
    private final long blocks;
    private final EnumSet<StatAttribute> attributeMask; // Supported attributes mask
    private final StatTimestamp accessTime; /* Last access time */
    private final StatTimestamp creationTime; /* File creation time */
    private final StatTimestamp attributeChangeTime; /* Last attribute change time */
    private final StatTimestamp modificationTime; /* Last data modification time */

    private final DeviceId rDevice;
    private final DeviceId fsDevice;

    private FileStat(final EnumSet<StatMask> mask,
                     final int blockSize,
                     final EnumSet<StatAttribute> attributes,
                     final int numLinks,
                     final int ownerUID,
                     final int ownerGID,
                     final FileType fileType,
                     final EnumSet<FilePermission> permissions,
                     final long inode,
                     final long size,
                     final long blocks,
                     final EnumSet<StatAttribute> attributeMask,
                     final StatTimestamp accessTime,
                     final StatTimestamp creationTime,
                     final StatTimestamp attributeChangeTime,
                     final StatTimestamp modificationTime,
                     final DeviceId rDevice,
                     final DeviceId fsDevice) {
        this.mask = mask;
        this.blockSize = blockSize;
        this.attributes = attributes;
        this.numLinks = numLinks;
        this.ownerUID = ownerUID;
        this.ownerGID = ownerGID;
        this.fileType = fileType;
        this.permissions = permissions;
        this.inode = inode;
        this.size = size;
        this.blocks = blocks;
        this.attributeMask = attributeMask;
        this.accessTime = accessTime;
        this.creationTime = creationTime;
        this.attributeChangeTime = attributeChangeTime;
        this.modificationTime = modificationTime;
        this.rDevice = rDevice;
        this.fsDevice = fsDevice;
    }

    public static FileStat fileStat(final EnumSet<StatMask> mask,
                                    final int blockSize,
                                    final EnumSet<StatAttribute> attributes,
                                    final int numLinks,
                                    final int ownerUID,
                                    final int ownerGID,
                                    final FileType fileType,
                                    final EnumSet<FilePermission> permissions,
                                    final long inode,
                                    final long size,
                                    final long blocks,
                                    final EnumSet<StatAttribute> attributeMask,
                                    final StatTimestamp accessTime,
                                    final StatTimestamp creationTime,
                                    final StatTimestamp attributeChangeTime,
                                    final StatTimestamp modificationTime,
                                    final DeviceId rDevice,
                                    final DeviceId fsDevice) {
        return new FileStat(mask,
                            blockSize,
                            attributes,
                            numLinks,
                            ownerUID,
                            ownerGID,
                            fileType,
                            permissions,
                            inode,
                            size,
                            blocks,
                            attributeMask,
                            accessTime,
                            creationTime,
                            attributeChangeTime,
                            modificationTime,
                            rDevice,
                            fsDevice);
    }

    public EnumSet<StatMask> mask() {
        return mask;
    }

    public int blockSize() {
        return blockSize;
    }

    public EnumSet<StatAttribute> attributes() {
        return attributes;
    }

    public int numLinks() {
        return numLinks;
    }

    public int ownerUID() {
        return ownerUID;
    }

    public int ownerGID() {
        return ownerGID;
    }

    public FileType fileType() {
        return fileType;
    }

    public EnumSet<FilePermission> permissions() {
        return permissions;
    }

    public long inode() {
        return inode;
    }

    public long size() {
        return size;
    }

    public long blocks() {
        return blocks;
    }

    public EnumSet<StatAttribute> attributeMask() {
        return attributeMask;
    }

    public StatTimestamp accessTime() {
        return accessTime;
    }

    public StatTimestamp creationTime() {
        return creationTime;
    }

    public StatTimestamp attributeChangeTime() {
        return attributeChangeTime;
    }

    public StatTimestamp modificationTime() {
        return modificationTime;
    }

    public DeviceId rDevice() {
        return rDevice;
    }

    public DeviceId fsDevice() {
        return fsDevice;
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

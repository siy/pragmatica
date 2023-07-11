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

package org.pragmatica.io.async.uring.struct.raw;

import org.pragmatica.io.async.file.FilePermission;
import org.pragmatica.io.async.file.stat.FileStat;
import org.pragmatica.io.async.file.stat.FileType;
import org.pragmatica.io.async.file.stat.StatAttribute;
import org.pragmatica.io.async.file.stat.StatMask;
import org.pragmatica.io.async.uring.struct.AbstractExternalRawStructure;
import org.pragmatica.io.async.uring.struct.shape.StatxOffsets;

import static org.pragmatica.io.async.file.stat.DeviceId.deviceId;

/**
 * File status information storage.
 */
public class RawStatx extends AbstractExternalRawStructure<RawStatx> {
    private final RawStatxTimestamp atime = RawStatxTimestamp.at(0);
    private final RawStatxTimestamp btime = RawStatxTimestamp.at(0);
    private final RawStatxTimestamp ctime = RawStatxTimestamp.at(0);
    private final RawStatxTimestamp mtime = RawStatxTimestamp.at(0);

    private RawStatx(long address) {
        super(address, StatxOffsets.SIZE);

        repositionInner(address);
    }

    public static RawStatx at(long address) {
        return new RawStatx(address);
    }

    private void repositionInner(long address) {
        atime.reposition(address + StatxOffsets.stx_atime.offset());
        btime.reposition(address + StatxOffsets.stx_btime.offset());
        ctime.reposition(address + StatxOffsets.stx_ctime.offset());
        mtime.reposition(address + StatxOffsets.stx_mtime.offset());
    }

    @Override
    public void reposition(long address) {
        repositionInner(address);
        super.reposition(address);
    }

    /** What results were written [uncond] */
    public int mask() {
        return getInt(StatxOffsets.stx_mask);
    }

    /** Preferred general I/O size [uncond] */
    public int blockSize() {
        return getInt(StatxOffsets.stx_blksize);
    }

    /** Flags conveying information about the file [uncond] */
    public long attributes() {
        return getLong(StatxOffsets.stx_attributes);
    }

    /** Number of hard links */
    public int numLinks() {
        return getInt(StatxOffsets.stx_nlink);
    }

    /** User ID of owner */
    public int ownerUID() {
        return getInt(StatxOffsets.stx_uid);
    }

    /** Group ID of owner */
    public int ownerGID() {
        return getInt(StatxOffsets.stx_gid);
    }

    /** File mode */
    public short mode() {
        return getShort(StatxOffsets.stx_mode);
    }

    /** Inode number */
    public long inode() {
        return getLong(StatxOffsets.stx_ino);
    }

    /** File size */
    public long fileSize() {
        return getLong(StatxOffsets.stx_size);
    }

    /** Number of 512-byte blocks allocated */
    public long blocks() {
        return getLong(StatxOffsets.stx_blocks);
    }

    /** Mask to show what's supported in stx_attributes */
    public long attributesMask() {
        return getLong(StatxOffsets.stx_attributes_mask);
    }

    /** Device ID of special file [if bdev/cdev] */
    public int rdevMajor() {
        return getInt(StatxOffsets.stx_rdev_major);
    }

    /** Device ID of special file [if bdev/cdev] */
    public int rdevMinor() {
        return getInt(StatxOffsets.stx_rdev_minor);
    }

    /** ID of device containing file [uncond] */
    public int devMajor() {
        return getInt(StatxOffsets.stx_dev_major);
    }

    /** ID of device containing file [uncond] */
    public int devMinor() {
        return getInt(StatxOffsets.stx_dev_minor);
    }

    /** Last access time */
    public RawStatxTimestamp accessTime() {
        return atime;
    }

    /** File creation time */
    public RawStatxTimestamp birthTime() {
        return btime;
    }

    /** Last metadata change time */
    public RawStatxTimestamp metadataChangeTime() {
        return ctime;
    }

    /** Last content modification time */
    public RawStatxTimestamp contentModificationTime() {
        return mtime;
    }

    public FileStat detach() {
        return FileStat.fileStat(
            StatMask.fromInt(mask()),
            blockSize(),
            StatAttribute.fromLong(attributes()),
            numLinks(),
            ownerUID(),
            ownerGID(),
            FileType.unsafeFromShort(mode()),
            FilePermission.fromShort(mode()),
            inode(),
            fileSize(),
            blocks(),
            StatAttribute.fromLong(attributesMask()),
            accessTime().detach(),
            birthTime().detach(),
            metadataChangeTime().detach(),
            contentModificationTime().detach(),
            deviceId(rdevMajor(), rdevMinor()),
            deviceId(devMajor(), devMinor()));
    }
}

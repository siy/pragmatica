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

package org.pfj.io.async.uring.struct.raw;

import org.pfj.io.async.file.FilePermission;
import org.pfj.io.async.file.stat.FileStat;
import org.pfj.io.async.file.stat.FileType;
import org.pfj.io.async.file.stat.StatAttribute;
import org.pfj.io.async.file.stat.StatMask;
import org.pfj.io.async.uring.struct.AbstractExternalRawStructure;
import org.pfj.io.async.uring.struct.shape.StatxOffsets;

import static org.pfj.io.async.file.stat.DeviceId.deviceId;
import static org.pfj.io.async.uring.struct.shape.StatxOffsets.stx_atime;
import static org.pfj.io.async.uring.struct.shape.StatxOffsets.stx_attributes;
import static org.pfj.io.async.uring.struct.shape.StatxOffsets.stx_attributes_mask;
import static org.pfj.io.async.uring.struct.shape.StatxOffsets.stx_blksize;
import static org.pfj.io.async.uring.struct.shape.StatxOffsets.stx_blocks;
import static org.pfj.io.async.uring.struct.shape.StatxOffsets.stx_btime;
import static org.pfj.io.async.uring.struct.shape.StatxOffsets.stx_ctime;
import static org.pfj.io.async.uring.struct.shape.StatxOffsets.stx_dev_major;
import static org.pfj.io.async.uring.struct.shape.StatxOffsets.stx_dev_minor;
import static org.pfj.io.async.uring.struct.shape.StatxOffsets.stx_gid;
import static org.pfj.io.async.uring.struct.shape.StatxOffsets.stx_ino;
import static org.pfj.io.async.uring.struct.shape.StatxOffsets.stx_mask;
import static org.pfj.io.async.uring.struct.shape.StatxOffsets.stx_mode;
import static org.pfj.io.async.uring.struct.shape.StatxOffsets.stx_mtime;
import static org.pfj.io.async.uring.struct.shape.StatxOffsets.stx_nlink;
import static org.pfj.io.async.uring.struct.shape.StatxOffsets.stx_rdev_major;
import static org.pfj.io.async.uring.struct.shape.StatxOffsets.stx_rdev_minor;
import static org.pfj.io.async.uring.struct.shape.StatxOffsets.stx_size;
import static org.pfj.io.async.uring.struct.shape.StatxOffsets.stx_uid;

public class RawStatx extends AbstractExternalRawStructure<RawStatx> {
    private final RawStatxTimestamp atime = RawStatxTimestamp.at(0);
    private final RawStatxTimestamp btime = RawStatxTimestamp.at(0);
    private final RawStatxTimestamp ctime = RawStatxTimestamp.at(0);
    private final RawStatxTimestamp mtime = RawStatxTimestamp.at(0);

    private RawStatx(final long address) {
        super(address, StatxOffsets.SIZE);

        repositionInner(address);
    }

    public static RawStatx at(final long address) {
        return new RawStatx(address);
    }

    private void repositionInner(final long address) {
        atime.reposition(address + stx_atime.offset());
        btime.reposition(address + stx_btime.offset());
        ctime.reposition(address + stx_ctime.offset());
        mtime.reposition(address + stx_mtime.offset());
    }

    @Override
    public RawStatx reposition(final long address) {
        repositionInner(address);
        return super.reposition(address);
    }

    /* What results were written [uncond] */
    public int mask() {
        return getInt(stx_mask);
    }

    /* Preferred general I/O size [uncond] */
    public int blockSize() {
        return getInt(stx_blksize);
    }

    /* Flags conveying information about the file [uncond] */
    public long attributes() {
        return getLong(stx_attributes);
    }

    /* Number of hard links */
    public int numLinks() {
        return getInt(stx_nlink);
    }

    /* User ID of owner */
    public int ownerUID() {
        return getInt(stx_uid);
    }

    /* Group ID of owner */
    public int ownerGID() {
        return getInt(stx_gid);
    }

    /* File mode */
    public short mode() {
        return getShort(stx_mode);
    }

    /* Inode number */
    public long inode() {
        return getLong(stx_ino);
    }

    /* File size */
    public long fileSize() {
        return getLong(stx_size);
    }

    /* Number of 512-byte blocks allocated */
    public long blocks() {
        return getLong(stx_blocks);
    }

    /* Mask to show what's supported in stx_attributes */
    public long attributesMask() {
        return getLong(stx_attributes_mask);
    }

    /* Device ID of special file [if bdev/cdev] */
    public int rdevMajor() {
        return getInt(stx_rdev_major);
    }

    /* Device ID of special file [if bdev/cdev] */
    public int rdevMinor() {
        return getInt(stx_rdev_minor);
    }

    /* ID of device containing file [uncond] */
    public int devMajor() {
        return getInt(stx_dev_major);
    }

    /* ID of device containing file [uncond] */
    public int devMinor() {
        return getInt(stx_dev_minor);
    }

    /* Last access time */
    public RawStatxTimestamp lastAccessTime() {
        return atime;
    }

    /* File creation time */
    public RawStatxTimestamp creationTime() {
        return btime;
    }

    /* Last attribute change time */
    public RawStatxTimestamp changeTime() {
        return ctime;
    }

    /* Last data modification time */
    public RawStatxTimestamp modificationTime() {
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
                lastAccessTime().detach(),
                creationTime().detach(),
                changeTime().detach(),
                modificationTime().detach(),
                deviceId(rdevMajor(), rdevMinor()),
                deviceId(devMajor(), devMinor()));
    }
}

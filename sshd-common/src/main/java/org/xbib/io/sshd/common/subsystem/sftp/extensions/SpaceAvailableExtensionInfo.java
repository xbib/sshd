package org.xbib.io.sshd.common.subsystem.sftp.extensions;

import org.xbib.io.sshd.common.util.NumberUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.io.IOException;
import java.nio.file.FileStore;

/**
 *
 */
public class SpaceAvailableExtensionInfo implements Cloneable {
    public long bytesOnDevice;
    public long unusedBytesOnDevice;
    public long bytesAvailableToUser;
    public long unusedBytesAvailableToUser;
    public int bytesPerAllocationUnit;

    public SpaceAvailableExtensionInfo() {
        super();
    }

    public SpaceAvailableExtensionInfo(Buffer buffer) {
        decode(buffer, this);
    }

    public SpaceAvailableExtensionInfo(FileStore store) throws IOException {
        bytesOnDevice = store.getTotalSpace();

        long unallocated = store.getUnallocatedSpace();
        long usable = store.getUsableSpace();
        unusedBytesOnDevice = Math.max(unallocated, usable);

        // the rest are intentionally  left zero indicating "UNKNOWN"
    }

    public static SpaceAvailableExtensionInfo decode(Buffer buffer) {
        SpaceAvailableExtensionInfo info = new SpaceAvailableExtensionInfo();
        decode(buffer, info);
        return info;
    }

    public static void decode(Buffer buffer, SpaceAvailableExtensionInfo info) {
        info.bytesOnDevice = buffer.getLong();
        info.unusedBytesOnDevice = buffer.getLong();
        info.bytesAvailableToUser = buffer.getLong();
        info.unusedBytesAvailableToUser = buffer.getLong();
        info.bytesPerAllocationUnit = buffer.getInt();
    }

    public static void encode(Buffer buffer, SpaceAvailableExtensionInfo info) {
        buffer.putLong(info.bytesOnDevice);
        buffer.putLong(info.unusedBytesOnDevice);
        buffer.putLong(info.bytesAvailableToUser);
        buffer.putLong(info.unusedBytesAvailableToUser);
        buffer.putInt(info.bytesPerAllocationUnit & 0xFFFFFFFFL);
    }

    @Override
    public int hashCode() {
        return NumberUtils.hashCode(bytesOnDevice, unusedBytesOnDevice,
                bytesAvailableToUser, unusedBytesAvailableToUser,
                bytesPerAllocationUnit);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        SpaceAvailableExtensionInfo other = (SpaceAvailableExtensionInfo) obj;
        return this.bytesOnDevice == other.bytesOnDevice
                && this.unusedBytesOnDevice == other.unusedBytesOnDevice
                && this.bytesAvailableToUser == other.bytesAvailableToUser
                && this.unusedBytesAvailableToUser == other.unusedBytesAvailableToUser
                && this.bytesPerAllocationUnit == other.bytesPerAllocationUnit;
    }

    @Override
    public SpaceAvailableExtensionInfo clone() {
        try {
            return getClass().cast(super.clone());
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to close " + toString() + ": " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "bytesOnDevice=" + bytesOnDevice
                + ",unusedBytesOnDevice=" + unusedBytesOnDevice
                + ",bytesAvailableToUser=" + bytesAvailableToUser
                + ",unusedBytesAvailableToUser=" + unusedBytesAvailableToUser
                + ",bytesPerAllocationUnit=" + bytesPerAllocationUnit;
    }
}

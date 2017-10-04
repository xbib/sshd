package org.xbib.io.sshd.client.subsystem.sftp.extensions.helpers;

import org.xbib.io.sshd.client.subsystem.sftp.RawSftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClient.Handle;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.CopyDataExtension;
import org.xbib.io.sshd.common.subsystem.sftp.SftpConstants;
import org.xbib.io.sshd.common.util.NumberUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.io.IOException;
import java.util.Collection;

/**
 * Implements the &quot;copy-data&quot; extension.
 */
public class CopyDataExtensionImpl extends AbstractSftpClientExtension implements CopyDataExtension {
    public CopyDataExtensionImpl(SftpClient client, RawSftpClient raw, Collection<String> extra) {
        super(SftpConstants.EXT_COPY_DATA, client, raw, extra);
    }

    @Override
    public void copyData(Handle readHandle, long readOffset, long readLength, Handle writeHandle, long writeOffset) throws IOException {
        byte[] srcId = readHandle.getIdentifier();
        byte[] dstId = writeHandle.getIdentifier();
        Buffer buffer = getCommandBuffer(Integer.BYTES + NumberUtils.length(srcId)
                + Integer.BYTES + NumberUtils.length(dstId)
                + (3 * (Long.SIZE + Integer.BYTES)));
        buffer.putBytes(srcId);
        buffer.putLong(readOffset);
        buffer.putLong(readLength);
        buffer.putBytes(dstId);
        buffer.putLong(writeOffset);
        sendAndCheckExtendedCommandStatus(buffer);
    }
}

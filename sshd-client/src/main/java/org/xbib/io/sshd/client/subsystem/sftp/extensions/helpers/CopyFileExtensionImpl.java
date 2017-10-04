package org.xbib.io.sshd.client.subsystem.sftp.extensions.helpers;

import org.xbib.io.sshd.client.subsystem.sftp.RawSftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.CopyFileExtension;
import org.xbib.io.sshd.common.subsystem.sftp.SftpConstants;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.io.IOException;
import java.util.Collection;

/**
 * Implements the &quot;copy-file&quot; extension.
 */
public class CopyFileExtensionImpl extends AbstractSftpClientExtension implements CopyFileExtension {
    public CopyFileExtensionImpl(SftpClient client, RawSftpClient raw, Collection<String> extra) {
        super(SftpConstants.EXT_COPY_FILE, client, raw, extra);
    }

    @Override
    public void copyFile(String src, String dst, boolean overwriteDestination) throws IOException {
        Buffer buffer = getCommandBuffer(Integer.BYTES + GenericUtils.length(src)
                + Integer.BYTES + GenericUtils.length(dst)
                + 1 /* override destination */);
        buffer.putString(src);
        buffer.putString(dst);
        buffer.putBoolean(overwriteDestination);
        sendAndCheckExtendedCommandStatus(buffer);
    }
}

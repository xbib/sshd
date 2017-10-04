package org.xbib.io.sshd.client.subsystem.sftp.extensions.helpers;

import org.xbib.io.sshd.client.subsystem.sftp.RawSftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClient.Handle;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.CheckFileHandleExtension;
import org.xbib.io.sshd.common.subsystem.sftp.SftpConstants;
import org.xbib.io.sshd.common.util.Pair;

import java.io.IOException;
import java.util.Collection;

/**
 * Implements &quot;check-file-handle&quot; extension.
 */
public class CheckFileHandleExtensionImpl extends AbstractCheckFileExtension implements CheckFileHandleExtension {
    public CheckFileHandleExtensionImpl(SftpClient client, RawSftpClient raw, Collection<String> extras) {
        super(SftpConstants.EXT_CHECK_FILE_HANDLE, client, raw, extras);
    }

    @Override
    public Pair<String, Collection<byte[]>> checkFileHandle(Handle handle, Collection<String> algorithms, long startOffset, long length, int blockSize) throws IOException {
        return doGetHash(handle.getIdentifier(), algorithms, startOffset, length, blockSize);
    }
}

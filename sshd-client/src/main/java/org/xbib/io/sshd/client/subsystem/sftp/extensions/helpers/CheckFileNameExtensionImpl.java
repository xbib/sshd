package org.xbib.io.sshd.client.subsystem.sftp.extensions.helpers;

import org.xbib.io.sshd.client.subsystem.sftp.RawSftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.CheckFileNameExtension;
import org.xbib.io.sshd.common.subsystem.sftp.SftpConstants;
import org.xbib.io.sshd.common.util.Pair;

import java.io.IOException;
import java.util.Collection;

/**
 * Implements &quot;check-file-name&quot; extension.
 */
public class CheckFileNameExtensionImpl extends AbstractCheckFileExtension implements CheckFileNameExtension {
    public CheckFileNameExtensionImpl(SftpClient client, RawSftpClient raw, Collection<String> extras) {
        super(SftpConstants.EXT_CHECK_FILE_NAME, client, raw, extras);
    }

    @Override
    public Pair<String, Collection<byte[]>> checkFileName(String name, Collection<String> algorithms, long startOffset, long length, int blockSize) throws IOException {
        return doGetHash(name, algorithms, startOffset, length, blockSize);
    }
}

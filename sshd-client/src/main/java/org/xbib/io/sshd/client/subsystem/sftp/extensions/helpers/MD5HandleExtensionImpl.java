package org.xbib.io.sshd.client.subsystem.sftp.extensions.helpers;

import org.xbib.io.sshd.client.subsystem.sftp.RawSftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.MD5HandleExtension;
import org.xbib.io.sshd.common.subsystem.sftp.SftpConstants;

import java.io.IOException;
import java.util.Collection;

/**
 * Implements &quot;md5-hash-handle&quot; extension.
 */
public class MD5HandleExtensionImpl extends AbstractMD5HashExtension implements MD5HandleExtension {
    public MD5HandleExtensionImpl(SftpClient client, RawSftpClient raw, Collection<String> extra) {
        super(SftpConstants.EXT_MD5_HASH_HANDLE, client, raw, extra);
    }

    @Override
    public byte[] getHash(SftpClient.Handle handle, long offset, long length, byte[] quickHash) throws IOException {
        return doGetHash(handle.getIdentifier(), offset, length, quickHash);
    }

}

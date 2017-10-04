package org.xbib.io.sshd.client.subsystem.sftp.extensions.helpers;

import org.xbib.io.sshd.client.subsystem.sftp.RawSftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.MD5FileExtension;
import org.xbib.io.sshd.common.subsystem.sftp.SftpConstants;

import java.io.IOException;
import java.util.Collection;

/**
 * Implements &quot;md5-hash&quot; extension.
 */
public class MD5FileExtensionImpl extends AbstractMD5HashExtension implements MD5FileExtension {
    public MD5FileExtensionImpl(SftpClient client, RawSftpClient raw, Collection<String> extra) {
        super(SftpConstants.EXT_MD5_HASH, client, raw, extra);
    }

    @Override
    public byte[] getHash(String path, long offset, long length, byte[] quickHash) throws IOException {
        return doGetHash(path, offset, length, quickHash);
    }
}

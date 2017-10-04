package org.xbib.io.sshd.client.subsystem.sftp.extensions.openssh.helpers;

import org.xbib.io.sshd.client.subsystem.sftp.RawSftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClient.Handle;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.openssh.OpenSSHStatExtensionInfo;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.openssh.OpenSSHStatHandleExtension;
import org.xbib.io.sshd.common.subsystem.sftp.extensions.openssh.FstatVfsExtensionParser;

import java.io.IOException;
import java.util.Map;

/**
 */
public class OpenSSHStatHandleExtensionImpl extends AbstractOpenSSHStatCommandExtension implements OpenSSHStatHandleExtension {
    public OpenSSHStatHandleExtensionImpl(SftpClient client, RawSftpClient raw, Map<String, byte[]> extensions) {
        super(FstatVfsExtensionParser.NAME, client, raw, extensions);
    }

    @Override
    public OpenSSHStatExtensionInfo stat(Handle handle) throws IOException {
        return doGetStat(handle.getIdentifier());
    }
}

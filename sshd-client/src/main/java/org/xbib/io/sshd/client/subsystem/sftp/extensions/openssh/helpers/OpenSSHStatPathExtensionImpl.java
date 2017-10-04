package org.xbib.io.sshd.client.subsystem.sftp.extensions.openssh.helpers;

import org.xbib.io.sshd.client.subsystem.sftp.RawSftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.openssh.OpenSSHStatExtensionInfo;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.openssh.OpenSSHStatPathExtension;
import org.xbib.io.sshd.common.subsystem.sftp.extensions.openssh.StatVfsExtensionParser;

import java.io.IOException;
import java.util.Map;

/**
 */
public class OpenSSHStatPathExtensionImpl extends AbstractOpenSSHStatCommandExtension implements OpenSSHStatPathExtension {
    public OpenSSHStatPathExtensionImpl(SftpClient client, RawSftpClient raw, Map<String, byte[]> extensions) {
        super(StatVfsExtensionParser.NAME, client, raw, extensions);
    }

    @Override
    public OpenSSHStatExtensionInfo stat(String path) throws IOException {
        return doGetStat(path);
    }
}

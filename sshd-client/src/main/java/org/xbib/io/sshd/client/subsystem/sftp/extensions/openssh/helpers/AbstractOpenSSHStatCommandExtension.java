package org.xbib.io.sshd.client.subsystem.sftp.extensions.openssh.helpers;

import org.xbib.io.sshd.client.subsystem.sftp.RawSftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.helpers.AbstractSftpClientExtension;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.openssh.OpenSSHStatExtensionInfo;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.Map;

/**
 *
 */
public abstract class AbstractOpenSSHStatCommandExtension extends AbstractSftpClientExtension {
    protected AbstractOpenSSHStatCommandExtension(String name, SftpClient client, RawSftpClient raw, Map<String, byte[]> extensions) {
        super(name, client, raw, extensions);
    }

    protected OpenSSHStatExtensionInfo doGetStat(Object target) throws IOException {
        Buffer buffer = getCommandBuffer(target);
        putTarget(buffer, target);
        buffer = checkExtendedReplyBuffer(receive(sendExtendedCommand(buffer)));
        if (buffer == null) {
            throw new StreamCorruptedException("Missing extended reply data");
        }

        return new OpenSSHStatExtensionInfo(buffer);
    }
}

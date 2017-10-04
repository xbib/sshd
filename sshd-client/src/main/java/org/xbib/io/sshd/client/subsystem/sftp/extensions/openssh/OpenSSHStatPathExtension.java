package org.xbib.io.sshd.client.subsystem.sftp.extensions.openssh;

import org.xbib.io.sshd.client.subsystem.sftp.extensions.SftpClientExtension;

import java.io.IOException;

/**
 * Implements the &quot;statvfs@openssh.com&quot; extension command.
 */
public interface OpenSSHStatPathExtension extends SftpClientExtension {
    OpenSSHStatExtensionInfo stat(String path) throws IOException;
}

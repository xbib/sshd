package org.xbib.io.sshd.client.subsystem.sftp.extensions.openssh;

import org.xbib.io.sshd.client.subsystem.sftp.SftpClient.Handle;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.SftpClientExtension;

import java.io.IOException;

/**
 * Implements the &quot;fstatvfs@openssh.com&quot; extension command.
 */
public interface OpenSSHStatHandleExtension extends SftpClientExtension {
    OpenSSHStatExtensionInfo stat(Handle handle) throws IOException;
}

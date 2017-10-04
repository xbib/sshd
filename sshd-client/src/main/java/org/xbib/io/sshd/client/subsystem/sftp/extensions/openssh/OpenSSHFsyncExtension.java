package org.xbib.io.sshd.client.subsystem.sftp.extensions.openssh;

import org.xbib.io.sshd.client.subsystem.sftp.SftpClient.Handle;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.SftpClientExtension;

import java.io.IOException;

/**
 * Implements the &quot;fsync@openssh.com&quot; extension.
 */
public interface OpenSSHFsyncExtension extends SftpClientExtension {
    void fsync(Handle fileHandle) throws IOException;
}

package org.xbib.io.sshd.client.subsystem.sftp.extensions;

import org.xbib.io.sshd.common.subsystem.sftp.extensions.SpaceAvailableExtensionInfo;

import java.io.IOException;

/**
 * Implements the &quot;space-available&quot; extension.
 */
public interface SpaceAvailableExtension extends SftpClientExtension {
    SpaceAvailableExtensionInfo available(String path) throws IOException;
}

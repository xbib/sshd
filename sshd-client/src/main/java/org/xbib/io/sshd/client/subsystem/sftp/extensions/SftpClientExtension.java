package org.xbib.io.sshd.client.subsystem.sftp.extensions;

import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.OptionalFeature;

/**
 */
public interface SftpClientExtension extends NamedResource, OptionalFeature {
    /**
     * @return The {@link SftpClient} used to issue the extended command
     */
    SftpClient getClient();
}

package org.xbib.io.sshd.client.simple;

import java.nio.channels.Channel;

/**
 * Provides a simplified and <U>synchronous</U> view of the available SSH client
 * functionality. If more fine-grained control and configuration of the SSH client
 * behavior and features is required then the {@link org.xbib.io.sshd.client.SshClient} object should be used
 */
public interface SimpleClient
        extends SimpleClientConfigurator,
        SimpleSessionClient,
        SimpleScpClient,
        SimpleSftpClient,
        Channel {
    // marker interface
}

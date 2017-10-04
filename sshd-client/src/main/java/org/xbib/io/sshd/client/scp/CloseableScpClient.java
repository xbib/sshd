package org.xbib.io.sshd.client.scp;

import java.nio.channels.Channel;

/**
 * An {@link org.xbib.io.sshd.client.scp.ScpClient} wrapper that also closes the underlying session
 * when closed.
 */
public interface CloseableScpClient extends ScpClient, Channel {
    // Marker interface
}

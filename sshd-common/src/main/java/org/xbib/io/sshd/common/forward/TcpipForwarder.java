package org.xbib.io.sshd.common.forward;

import org.xbib.io.sshd.common.Closeable;
import org.xbib.io.sshd.common.util.net.SshdSocketAddress;

import java.io.IOException;

/**
 *
 */
public interface TcpipForwarder
        extends PortForwardingManager,
        PortForwardingEventListenerManager,
        PortForwardingEventListenerManagerHolder,
        Closeable {
    /**
     * @param remotePort The remote port
     * @return The local {@link SshdSocketAddress} that the remote port is forwarded to
     */
    SshdSocketAddress getForwardedPort(int remotePort);

    /**
     * Called when the other side requested a remote port forward.
     *
     * @param local The request address
     * @return The bound local {@link SshdSocketAddress} - {@code null} if not allowed to forward
     * @throws IOException If failed to handle request
     */
    SshdSocketAddress localPortForwardingRequested(SshdSocketAddress local) throws IOException;

    /**
     * Called when the other side cancelled a remote port forward.
     *
     * @param local The local {@link SshdSocketAddress}
     * @throws IOException If failed to handle request
     */
    void localPortForwardingCancelled(SshdSocketAddress local) throws IOException;
}

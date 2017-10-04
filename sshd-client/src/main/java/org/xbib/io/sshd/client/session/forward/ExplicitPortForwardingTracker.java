package org.xbib.io.sshd.client.session.forward;

import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.common.forward.PortForwardingManager;
import org.xbib.io.sshd.common.util.net.SshdSocketAddress;

import java.io.IOException;
import java.util.Objects;

/**
 *
 */
public class ExplicitPortForwardingTracker extends PortForwardingTracker {
    private final boolean localForwarding;
    private final SshdSocketAddress remoteAddress;

    public ExplicitPortForwardingTracker(ClientSession session, boolean localForwarding,
                                         SshdSocketAddress localAddress, SshdSocketAddress remoteAddress, SshdSocketAddress boundAddress) {
        super(session, localAddress, boundAddress);
        this.localForwarding = localForwarding;
        this.remoteAddress = Objects.requireNonNull(remoteAddress, "No remote address specified");
    }

    public boolean isLocalForwarding() {
        return localForwarding;
    }

    public SshdSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public void close() throws IOException {
        if (open.getAndSet(false)) {
            PortForwardingManager manager = getClientSession();
            if (isLocalForwarding()) {
                manager.stopLocalPortForwarding(getLocalAddress());
            } else {
                manager.stopRemotePortForwarding(getRemoteAddress());
            }
        }
    }

    @Override
    public String toString() {
        return super.toString()
                + "[localForwarding=" + isLocalForwarding()
                + ", remote=" + getRemoteAddress()
                + "]";
    }
}

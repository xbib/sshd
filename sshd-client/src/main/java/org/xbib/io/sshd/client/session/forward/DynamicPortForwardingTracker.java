package org.xbib.io.sshd.client.session.forward;

import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.common.forward.PortForwardingManager;
import org.xbib.io.sshd.common.util.net.SshdSocketAddress;

import java.io.IOException;

/**
 *
 */
public class DynamicPortForwardingTracker extends PortForwardingTracker {
    public DynamicPortForwardingTracker(ClientSession session, SshdSocketAddress localAddress, SshdSocketAddress boundAddress) {
        super(session, localAddress, boundAddress);
    }

    @Override
    public void close() throws IOException {
        if (open.getAndSet(false)) {
            PortForwardingManager manager = getClientSession();
            manager.stopDynamicPortForwarding(getBoundAddress());
        }
    }
}

package org.xbib.io.sshd.client.session.forward;

import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.client.session.ClientSessionHolder;
import org.xbib.io.sshd.common.util.net.SshdSocketAddress;

import java.nio.channels.Channel;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public abstract class PortForwardingTracker implements Channel, ClientSessionHolder {
    protected final AtomicBoolean open = new AtomicBoolean(true);
    private final ClientSession session;
    private final SshdSocketAddress localAddress;
    private final SshdSocketAddress boundAddress;

    protected PortForwardingTracker(ClientSession session, SshdSocketAddress localAddress, SshdSocketAddress boundAddress) {
        this.session = Objects.requireNonNull(session, "No client session provided");
        this.localAddress = Objects.requireNonNull(localAddress, "No local address specified");
        this.boundAddress = Objects.requireNonNull(boundAddress, "No bound address specified");
    }

    @Override
    public boolean isOpen() {
        return open.get();
    }

    public SshdSocketAddress getLocalAddress() {
        return localAddress;
    }

    public SshdSocketAddress getBoundAddress() {
        return boundAddress;
    }

    @Override
    public ClientSession getClientSession() {
        return session;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "[session=" + getClientSession()
                + ", localAddress=" + getLocalAddress()
                + ", boundAddress=" + getBoundAddress()
                + ", open=" + isOpen()
                + "]";
    }
}

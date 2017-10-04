package org.xbib.io.sshd.client.keyverifier;

import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

import java.net.SocketAddress;
import java.security.PublicKey;

/**
 * Returns the same constant answer {@code true/false} regardless.
 */
public abstract class StaticServerKeyVerifier extends AbstractLoggingBean implements ServerKeyVerifier {
    private final boolean acceptance;

    protected StaticServerKeyVerifier(boolean acceptance) {
        this.acceptance = acceptance;
    }

    public final boolean isAccepted() {
        return acceptance;
    }

    @Override
    public final boolean verifyServerKey(ClientSession sshClientSession, SocketAddress remoteAddress, PublicKey serverKey) {
        boolean accepted = isAccepted();
        if (accepted) {
            handleAcceptance(sshClientSession, remoteAddress, serverKey);
        } else {
            handleRejection(sshClientSession, remoteAddress, serverKey);
        }

        return accepted;
    }

    protected void handleAcceptance(ClientSession sshClientSession, SocketAddress remoteAddress, PublicKey serverKey) {
        // accepting without really checking is dangerous, thus the warning
    }

    protected void handleRejection(ClientSession sshClientSession, SocketAddress remoteAddress, PublicKey serverKey) {
    }

}

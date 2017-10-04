package org.xbib.io.sshd.server.auth.pubkey;

import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;
import org.xbib.io.sshd.server.session.ServerSession;

import java.security.PublicKey;

/**
 * Returns the same constant result {@code true/false} regardless.
 */
public abstract class StaticPublickeyAuthenticator extends AbstractLoggingBean implements PublickeyAuthenticator {
    private final boolean acceptance;

    protected StaticPublickeyAuthenticator(boolean acceptance) {
        this.acceptance = acceptance;
    }

    public final boolean isAccepted() {
        return acceptance;
    }

    @Override
    public final boolean authenticate(String username, PublicKey key, ServerSession session) {
        boolean accepted = isAccepted();
        if (accepted) {
            handleAcceptance(username, key, session);
        }

        return accepted;
    }

    protected void handleAcceptance(String username, PublicKey key, ServerSession session) {
    }

    protected void handleRejection(String username, PublicKey key, ServerSession session) {
    }
}

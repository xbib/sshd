package org.xbib.io.sshd.server.auth.password;

import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;
import org.xbib.io.sshd.server.session.ServerSession;

/**
 * Returns the same constant result {@code true/false} regardless.
 */
public class StaticPasswordAuthenticator extends AbstractLoggingBean implements PasswordAuthenticator {
    private final boolean acceptance;

    public StaticPasswordAuthenticator(boolean acceptance) {
        this.acceptance = acceptance;
    }

    public final boolean isAccepted() {
        return acceptance;
    }

    @Override
    public final boolean authenticate(String username, String password, ServerSession session) {
        boolean accepted = isAccepted();
        if (accepted) {
            handleAcceptance(username, password, session);
        } else {
            handleRejection(username, password, session);
        }

        return accepted;
    }

    protected void handleAcceptance(String username, String password, ServerSession session) {
        // accepting without really checking is dangerous, thus the warning
    }

    protected void handleRejection(String username, String password, ServerSession session) {
    }
}

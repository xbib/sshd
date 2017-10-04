package org.xbib.io.sshd.server.auth.hostbased;

import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;
import org.xbib.io.sshd.server.session.ServerSession;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 *
 */
public class StaticHostBasedAuthenticator extends AbstractLoggingBean implements HostBasedAuthenticator {
    private final boolean acceptance;

    public StaticHostBasedAuthenticator(boolean acceptance) {
        this.acceptance = acceptance;
    }

    public final boolean isAccepted() {
        return acceptance;
    }

    @Override
    public final boolean authenticate(ServerSession session, String username, PublicKey clientHostKey,
                                      String clientHostName, String clientUsername, List<X509Certificate> certificates) {
        boolean accepted = isAccepted();
        if (accepted) {
            handleAcceptance(session, username, clientHostKey, clientHostName, clientUsername, certificates);
        } else {
            handleRejection(session, username, clientHostKey, clientHostName, clientUsername, certificates);
        }

        return accepted;
    }

    protected void handleAcceptance(ServerSession session, String username, PublicKey clientHostKey,
                                    String clientHostName, String clientUsername, List<X509Certificate> certificates) {
        // accepting without really checking is dangerous, thus the warning
    }

    protected void handleRejection(ServerSession session, String username, PublicKey clientHostKey,
                                   String clientHostName, String clientUsername, List<X509Certificate> certificates) {
    }
}

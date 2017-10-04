package org.xbib.io.sshd.server.auth.hostbased;

/**
 *
 */
public final class RejectAllHostBasedAuthenticator extends StaticHostBasedAuthenticator {
    public static final RejectAllHostBasedAuthenticator INSTANCE = new RejectAllHostBasedAuthenticator();

    private RejectAllHostBasedAuthenticator() {
        super(false);
    }
}

package org.xbib.io.sshd.server.auth.hostbased;

/**
 *
 */
public final class AcceptAllHostBasedAuthenticator extends StaticHostBasedAuthenticator {
    public static final AcceptAllHostBasedAuthenticator INSTANCE = new AcceptAllHostBasedAuthenticator();

    private AcceptAllHostBasedAuthenticator() {
        super(true);
    }
}

package org.xbib.io.sshd.server.auth.password;

/**
 * Accepts all authentication attempts.
 */
public final class AcceptAllPasswordAuthenticator extends StaticPasswordAuthenticator {
    public static final AcceptAllPasswordAuthenticator INSTANCE = new AcceptAllPasswordAuthenticator();

    private AcceptAllPasswordAuthenticator() {
        super(true);
    }
}

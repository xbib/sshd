package org.xbib.io.sshd.server.auth.password;

/**
 * Rejects all authentication attempts.
 */
public final class RejectAllPasswordAuthenticator extends StaticPasswordAuthenticator {
    public static final RejectAllPasswordAuthenticator INSTANCE = new RejectAllPasswordAuthenticator();

    private RejectAllPasswordAuthenticator() {
        super(false);
    }
}

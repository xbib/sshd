package org.xbib.io.sshd.server.auth.pubkey;

/**
 * Rejects all authentication attempts.
 */
public final class RejectAllPublickeyAuthenticator extends StaticPublickeyAuthenticator {
    public static final RejectAllPublickeyAuthenticator INSTANCE = new RejectAllPublickeyAuthenticator();

    private RejectAllPublickeyAuthenticator() {
        super(false);
    }
}

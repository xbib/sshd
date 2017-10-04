package org.xbib.io.sshd.server.auth.pubkey;

/**
 * Accepts all authentication attempts.
 */
public final class AcceptAllPublickeyAuthenticator extends StaticPublickeyAuthenticator {
    public static final AcceptAllPublickeyAuthenticator INSTANCE = new AcceptAllPublickeyAuthenticator();

    private AcceptAllPublickeyAuthenticator() {
        super(true);
    }
}

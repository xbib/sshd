package org.xbib.io.sshd.client.keyverifier;

/**
 * A ServerKeyVerifier that accepts all server keys.
 */
public final class AcceptAllServerKeyVerifier extends StaticServerKeyVerifier {
    public static final AcceptAllServerKeyVerifier INSTANCE = new AcceptAllServerKeyVerifier();

    private AcceptAllServerKeyVerifier() {
        super(true);
    }
}

package org.xbib.io.sshd.client.keyverifier;

/**
 *
 */
public final class RejectAllServerKeyVerifier extends StaticServerKeyVerifier {
    public static final RejectAllServerKeyVerifier INSTANCE = new RejectAllServerKeyVerifier();

    private RejectAllServerKeyVerifier() {
        super(false);
    }
}

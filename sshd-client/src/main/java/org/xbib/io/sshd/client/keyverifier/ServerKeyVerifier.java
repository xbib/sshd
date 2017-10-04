package org.xbib.io.sshd.client.keyverifier;

import org.xbib.io.sshd.client.session.ClientSession;

import java.net.SocketAddress;
import java.security.PublicKey;

/**
 * The <code>ServerKeyVerifier</code> is used on the client side
 * to authenticate the key provided by the server.
 */
@FunctionalInterface
public interface ServerKeyVerifier {
    /**
     * Verify that the server key provided is really the one of the host.
     *
     * @param clientSession the current {@link ClientSession}
     * @param remoteAddress the host's {@link SocketAddress}
     * @param serverKey     the presented server {@link PublicKey}
     * @return <code>true</code> if the key is accepted for the host
     */
    boolean verifyServerKey(ClientSession clientSession, SocketAddress remoteAddress, PublicKey serverKey);
}

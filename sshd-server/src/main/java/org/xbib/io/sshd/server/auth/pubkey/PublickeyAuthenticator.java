package org.xbib.io.sshd.server.auth.pubkey;

import org.xbib.io.sshd.server.session.ServerSession;

import java.security.PublicKey;

/**
 * The <code>PublickeyAuthenticator</code> is used on the server side
 * to authenticate user public keys.
 */
@FunctionalInterface
public interface PublickeyAuthenticator {

    /**
     * Check the validity of a public key.
     *
     * @param username the username
     * @param key      the key
     * @param session  the server session
     * @return a boolean indicating if authentication succeeded or not
     */
    boolean authenticate(String username, PublicKey key, ServerSession session);
}

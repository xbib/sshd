package org.xbib.io.sshd.server.auth.password;

import org.xbib.io.sshd.server.session.ServerSession;

/**
 * Used to authenticate users based on a password.
 */
@FunctionalInterface
public interface PasswordAuthenticator {
    /**
     * Check the validity of a password.
     *
     * @param username The username credential
     * @param password The provided password
     * @param session  The {@link ServerSession} attempting the authentication
     * @return {@code true} indicating if authentication succeeded
     * @throws PasswordChangeRequiredException If the password is expired or
     *                                         not strong enough to suit the server's policy
     */
    boolean authenticate(String username, String password, ServerSession session) throws PasswordChangeRequiredException;
}

package org.xbib.io.sshd.server.auth.keyboard;

import org.xbib.io.sshd.server.session.ServerSession;

import java.util.List;

/**
 * Provides pluggable authentication using the &quot;keyboard-interactive&quot;
 * method as specified by <A HREF="https://www.ietf.org/rfc/rfc4256.txt">RFC-4256</A>.
 */
public interface KeyboardInteractiveAuthenticator {
    /**
     * An authenticator that rejects any attempt to use it
     */
    KeyboardInteractiveAuthenticator NONE = new KeyboardInteractiveAuthenticator() {
        @Override
        public org.xbib.io.sshd.server.auth.keyboard.InteractiveChallenge generateChallenge(ServerSession session, String username, String lang, String subMethods) {
            return null;
        }

        @Override
        public boolean authenticate(ServerSession session, String username, List<String> responses) throws Exception {
            return false;
        }

        @Override
        public String toString() {
            return "NONE";
        }
    };

    /**
     * Generates the interactive &quot;challenge&quot; to send to the client
     *
     * @param session    The {@link ServerSession} through which the request was received
     * @param username   The username
     * @param lang       The language tag
     * @param subMethods Sub-methods hints sent by the client
     * @return The {@link org.xbib.io.sshd.server.auth.keyboard.InteractiveChallenge} - if {@code null} then authentication
     * attempt via &quot;keyboard-interactive&quot; method is rejected
     */
    InteractiveChallenge generateChallenge(ServerSession session, String username, String lang, String subMethods);

    /**
     * Called to authenticate the response to the challenge(s) sent previously
     *
     * @param session   The {@link ServerSession} through which the response was received
     * @param username  The username
     * @param responses The received responses - <B>Note:</B> it is up to the authenticator
     *                  to make sure that the number of responses matches the number of prompts sent in
     *                  the initial challenge. The <U>order</U> of the responses matches the order of the
     *                  prompts sent to the client
     * @return {@code true} if responses have been validated
     * @throws Exception if bad responses and server should terminate the connection
     */
    boolean authenticate(ServerSession session, String username, List<String> responses) throws Exception;
}

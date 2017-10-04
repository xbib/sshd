package org.xbib.io.sshd.server.auth;

import org.xbib.io.sshd.common.auth.UserAuthInstance;
import org.xbib.io.sshd.common.auth.UsernameHolder;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.server.session.ServerSession;
import org.xbib.io.sshd.server.session.ServerSessionHolder;

/**
 * Server side authentication mechanism.
 */
public interface UserAuth extends ServerSessionHolder, UserAuthInstance<ServerSession>, UsernameHolder {

    /**
     * Try to authenticate the user. This methods should return a non {@code null}
     * value indicating if the authentication succeeded. If the authentication is
     * still ongoing, a {@code null} value should be returned.
     *
     * @param session  the current {@link ServerSession} session
     * @param username the user trying to log in
     * @param service  the requested service name
     * @param buffer   the request buffer containing parameters specific to this request
     * @return <code>true</code> if the authentication succeeded, <code>false</code> if the authentication
     * failed and {@code null} if not finished yet
     * @throws Exception if the authentication fails
     */
    Boolean auth(ServerSession session, String username, String service, Buffer buffer) throws Exception;

    /**
     * Handle another step in the authentication process.
     *
     * @param buffer the request buffer containing parameters specific to this request
     * @return <code>true</code> if the authentication succeeded, <code>false</code> if the authentication
     * failed and {@code null} if not finished yet
     * @throws Exception if the authentication fails
     */
    Boolean next(Buffer buffer) throws Exception;

    /**
     * Free any system resources used by the module.
     */
    void destroy();
}

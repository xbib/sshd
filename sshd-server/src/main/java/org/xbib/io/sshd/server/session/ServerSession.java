package org.xbib.io.sshd.server.session;

import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.server.ServerAuthenticationManager;
import org.xbib.io.sshd.server.ServerFactoryManager;

import java.net.SocketAddress;
import java.security.KeyPair;

/**
 *
 */
public interface ServerSession
        extends Session,
        ServerProxyAcceptorHolder,
        ServerAuthenticationManager {

    /**
     * @return The {@link ServerFactoryManager} for this session
     */
    @Override
    ServerFactoryManager getFactoryManager();

    /**
     * @return The {@link SocketAddress} of the remote client. If no proxy wrapping
     * was used then this is the same as the {@code IoSession#getRemoteAddress()}.
     * Otherwise, it indicates the real client's address that was somehow transmitted
     * via the proxy meta-data
     */
    SocketAddress getClientAddress();

    /**
     * @return The {@link KeyPair} representing the current session's used keys
     * on KEX - {@code null} if not negotiated yet
     */
    KeyPair getHostKey();

    /**
     * Retrieve the current number of sessions active for a given username.
     *
     * @param userName The name of the user - ignored if {@code null}/empty
     * @return The current number of live <code>SshSession</code> objects associated with the user
     */
    int getActiveSessionCountForUser(String userName);
}

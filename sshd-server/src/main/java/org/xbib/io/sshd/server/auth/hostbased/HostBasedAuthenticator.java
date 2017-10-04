package org.xbib.io.sshd.server.auth.hostbased;

import org.xbib.io.sshd.server.session.ServerSession;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Invoked when &quot;hostbased&quot; authentication is used.
 */
@FunctionalInterface
public interface HostBasedAuthenticator {
    /**
     * @param session        The {@link ServerSession} through which the request was received
     * @param username       The username attempting to login
     * @param clientHostKey  The remote client's host {@link PublicKey}
     * @param clientHostName The reported remote client's host name
     * @param clientUsername The remote client username
     * @param certificates   Associated {@link X509Certificate}s - may be {@code null}/empty
     * @return {@code true} whether authentication is allowed to proceed
     */
    boolean authenticate(ServerSession session, String username,
                         PublicKey clientHostKey, String clientHostName, String clientUsername, List<X509Certificate> certificates);
}

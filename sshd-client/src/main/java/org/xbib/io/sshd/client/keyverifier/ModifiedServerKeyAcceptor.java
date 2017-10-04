package org.xbib.io.sshd.client.keyverifier;

import org.xbib.io.sshd.client.config.hosts.KnownHostEntry;
import org.xbib.io.sshd.client.session.ClientSession;

import java.net.SocketAddress;
import java.security.PublicKey;

/**
 */
@FunctionalInterface
public interface ModifiedServerKeyAcceptor {
    /**
     * Invoked when a matching known host key was found but it does not match
     * the presented one.
     *
     * @param clientSession The {@link ClientSession}
     * @param remoteAddress The remote host address
     * @param entry         The original {@link KnownHostEntry} whose key did not match
     * @param expected      The expected server {@link PublicKey}
     * @param actual        The presented server {@link PublicKey}
     * @return {@code true} if accept the server key anyway
     * @throws Exception if cannot process the request - equivalent to {@code false} return value
     */
    boolean acceptModifiedServerKey(ClientSession clientSession, SocketAddress remoteAddress,
                                    KnownHostEntry entry, PublicKey expected, PublicKey actual)
            throws Exception;
}

package org.xbib.io.sshd.client.session;

/**
 * Provides a way to implement proxied connections where some metadata
 * about the client is sent <U>before</U> the actual SSH protocol is
 * executed.
 * The implementor should use the {@code IoSession#write(Buffer)} method
 * to send any packets with the meta-data.
 */
@FunctionalInterface
public interface ClientProxyConnector {
    /**
     * Invoked just before the client identification is sent so that the
     * proxy can send the meta-data to its peer. Upon successful return
     * the SSH identification line is sent and the protocol proceeds as usual.
     *
     * @param session The {@link org.xbib.io.sshd.client.session.ClientSession} instance
     * @throws Exception If failed to send the data - which will also
     *                   terminate the session
     */
    void sendClientProxyMetadata(ClientSession session) throws Exception;
}

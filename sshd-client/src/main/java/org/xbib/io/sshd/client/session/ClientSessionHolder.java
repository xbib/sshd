package org.xbib.io.sshd.client.session;

/**
 */
@FunctionalInterface
public interface ClientSessionHolder {
    /**
     * @return The underlying {@link org.xbib.io.sshd.client.session.ClientSession} used
     */
    ClientSession getClientSession();
}

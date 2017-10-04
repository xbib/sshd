package org.xbib.io.sshd.server.session;

/**
 *
 */
@FunctionalInterface
public interface ServerSessionHolder {
    /**
     * @return The underlying {@link org.xbib.io.sshd.server.session.ServerSession} used
     */
    ServerSession getServerSession();
}

package org.xbib.io.sshd.server;

import org.xbib.io.sshd.server.session.ServerSession;

/**
 * Interface that can be implemented by a command to be able to access the
 * server session in which this command will be used.
 */
@FunctionalInterface
public interface SessionAware {

    /**
     * @param session The {@link ServerSession} in which this shell will be executed.
     */
    void setSession(ServerSession session);
}

package org.xbib.io.sshd.common.io;

import org.xbib.io.sshd.common.Closeable;

import java.util.Map;

/**
 *
 */
public interface IoService extends Closeable {
    /**
     * Socket reuse address.
     * See {@link java.net.StandardSocketOptions#SO_REUSEADDR}
     */
    boolean DEFAULT_REUSE_ADDRESS = true;

    /**
     * Returns the map of all sessions which are currently managed by this
     * service.  The key of map is the {@link IoSession#getId() ID} of the
     * session.
     *
     * @return the sessions. An empty collection if there's no session.
     */
    Map<Long, IoSession> getManagedSessions();

}

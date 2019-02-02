package org.xbib.io.sshd.common.io;

import org.xbib.io.sshd.common.Closeable;

import java.net.SocketAddress;

public interface IoSession extends PacketWriter, Closeable {

    /**
     * @return a unique identifier for this session.  Every session has its own
     * ID which is different from each other.
     */
    long getId();

    /**
     * Returns the value of the user-defined attribute of this session.
     *
     * @param key the key of the attribute
     * @return <tt>null</tt> if there is no attribute with the specified key
     */
    Object getAttribute(Object key);

    /**
     * Sets a user-defined attribute.
     *
     * @param key   the key of the attribute
     * @param value the value of the attribute
     * @return The old value of the attribute.  <tt>null</tt> if it is new.
     */
    Object setAttribute(Object key, Object value);

    /**
     * @return the socket address of remote peer.
     */
    SocketAddress getRemoteAddress();

    /**
     * @return the socket address of local machine which is associated with this
     * session.
     */
    SocketAddress getLocalAddress();

    /**
     * @return the {@link IoService} that created this session.
     */
    IoService getService();
}

package org.xbib.io.sshd.common.io;

import org.xbib.io.sshd.common.Closeable;
import org.xbib.io.sshd.common.future.CloseFuture;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.net.SocketAddress;

public interface IoSession extends Closeable {

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
     * Write a packet on the socket.
     * Multiple writes can be issued concurrently and will be queued.
     *
     * @param buffer The {@link Buffer} with the encoded packet data
     * @return The {@link IoWriteFuture} for the request
     */
    IoWriteFuture write(Buffer buffer);

    /**
     * Closes this session immediately or after all queued write requests
     * are flushed.  This operation is asynchronous.  Wait for the returned
     * {@link CloseFuture} if you want to wait for the session actually closed.
     *
     * @param immediately {@code true} to close this session immediately.
     *                    The pending write requests will simply be discarded.
     *                    {@code false} to close this session after all queued
     *                    write requests are flushed.
     * @return The generated {@link CloseFuture}
     */
    @Override
    CloseFuture close(boolean immediately);

    /**
     * @return the {@link org.xbib.io.sshd.common.io.IoService} that created this session.
     */
    IoService getService();
}

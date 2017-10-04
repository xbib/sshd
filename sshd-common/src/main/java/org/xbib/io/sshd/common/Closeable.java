package org.xbib.io.sshd.common;

import org.xbib.io.sshd.common.future.CloseFuture;
import org.xbib.io.sshd.common.future.SshFutureListener;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.channels.Channel;
import java.util.concurrent.TimeUnit;

/**
 * A {@code Closeable} is a resource that can be closed.
 * The close method is invoked to release resources that the object is
 * holding. The user can pre-register listeners to be notified
 * when resource close is completed (successfully or otherwise).
 */
public interface Closeable extends Channel {

    /**
     * Timeout (milliseconds) for waiting on a {@link CloseFuture} to successfully
     * complete its action.
     *
     * @see #DEFAULT_CLOSE_WAIT_TIMEOUT
     */
    String CLOSE_WAIT_TIMEOUT = "sshd-close-wait-time";

    /**
     * Default value for {@link #CLOSE_WAIT_TIMEOUT} if none specified
     */
    long DEFAULT_CLOSE_WAIT_TIMEOUT = TimeUnit.SECONDS.toMillis(15L);

    static long getMaxCloseWaitTime(PropertyResolver resolver) {
        return (resolver == null) ? DEFAULT_CLOSE_WAIT_TIMEOUT
                : resolver.getLongProperty(CLOSE_WAIT_TIMEOUT, DEFAULT_CLOSE_WAIT_TIMEOUT);
    }

    static void close(Closeable closeable) throws IOException {
        if (closeable == null) {
            return;
        }

        if ((!closeable.isClosed()) && (!closeable.isClosing())) {
            CloseFuture future = closeable.close(true);
            long maxWait = (closeable instanceof PropertyResolver)
                    ? getMaxCloseWaitTime((PropertyResolver) closeable) : DEFAULT_CLOSE_WAIT_TIMEOUT;
            boolean successful = future.await(maxWait);
            if (!successful) {
                throw new SocketTimeoutException("Failed to receive closure confirmation within " + maxWait + " millis");
            }
        }
    }

    /**
     * Close this resource asynchronously and return a future.
     * Resources support two closing modes: a graceful mode
     * which will cleanly close the resource and an immediate mode
     * which will close the resources abruptly.
     *
     * @param immediately <code>true</code> if the resource should be shut down abruptly,
     *                    <code>false</code> for a graceful close
     * @return a {@link CloseFuture} representing the close request
     */
    CloseFuture close(boolean immediately);

    /**
     * Pre-register a listener to be informed when resource is closed. If
     * resource is already closed, the listener will be invoked immediately
     * and not registered for future notification
     *
     * @param listener The notification {@link SshFutureListener} - never {@code null}
     */
    void addCloseFutureListener(SshFutureListener<CloseFuture> listener);

    /**
     * Remove a pre-registered close event listener
     *
     * @param listener The register {@link SshFutureListener} - never {@code null}.
     *                 Ignored if not registered or resource already closed
     */
    void removeCloseFutureListener(SshFutureListener<CloseFuture> listener);

    /**
     * Returns <code>true</code> if this object has been closed.
     *
     * @return <code>true</code> if closing
     */
    boolean isClosed();

    /**
     * Returns <code>true</code> if the {@link #close(boolean)} method
     * has been called. Note that this method will return <code>true</code>
     * even if this {@link #isClosed()} returns <code>true</code>.
     *
     * @return <code>true</code> if closing
     */
    boolean isClosing();

    @Override
    default boolean isOpen() {
        return !(isClosed() || isClosing());
    }

    @Override
    default void close() throws IOException {
        Closeable.close(this);
    }
}

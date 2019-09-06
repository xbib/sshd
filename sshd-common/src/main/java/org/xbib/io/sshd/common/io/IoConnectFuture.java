package org.xbib.io.sshd.common.io;

import org.xbib.io.sshd.common.future.SshFuture;

/**
 *
 */
public interface IoConnectFuture extends SshFuture<IoConnectFuture> {

    /**
     * @return The current {@link IoSession} - may be {@code null}
     * if connect operation not finished yet or attempt has failed
     * @see #getException()
     */
    IoSession getSession();

    /**
     * Sets the newly connected session and notifies all threads waiting for
     * this future.  This method is invoked by SSHD internally.  Please do not
     * call this method directly.
     *
     * @param session The connected {@link IoSession}
     */
    void setSession(IoSession session);

    /**
     * Returns the cause of the connection failure.
     *
     * @return {@code null} if the connect operation is not finished yet,
     * or if the connection attempt is successful.
     * @see #getSession()
     */
    Throwable getException();

    /**
     * Sets the exception caught due to connection failure and notifies all
     * threads waiting for this future.  This method is invoked by SSHD
     * internally.  Please do not call this method directly.
     *
     * @param exception The caught {@link Throwable}
     */
    void setException(Throwable exception);

    /**
     * @return {@code true} if the connect operation is finished successfully.
     */
    boolean isConnected();

    /**
     * @return {@code true} if the connect operation has been canceled by
     * {@link #cancel()} method.
     */
    boolean isCanceled();

    /**
     * Cancels the connection attempt and notifies all threads waiting for
     * this future.
     */
    void cancel();
}

package org.xbib.io.sshd.client.future;

import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.common.future.SshFuture;
import org.xbib.io.sshd.common.future.VerifiableFuture;

/**
 * An {@link SshFuture} for asynchronous connections requests.
 */
public interface ConnectFuture extends SshFuture<ConnectFuture>, VerifiableFuture<ConnectFuture> {
    /**
     * @return The referenced {@link ClientSession}
     */
    ClientSession getSession();

    /**
     * Sets the newly connected session and notifies all threads waiting for
     * this future.  This method is invoked by SSHD internally.  Please do not
     * call this method directly.
     *
     * @param session The {@link ClientSession}
     */
    void setSession(ClientSession session);

    /**
     * @return <code>true</code> if the connect operation is finished successfully.
     */
    boolean isConnected();

    /**
     * @return {@code true} if the connect operation has been canceled by
     * {@link #cancel()} method.
     */
    boolean isCanceled();

    /**
     * Returns the cause of the connection failure.
     *
     * @return <code>null</code> if the connect operation is not finished yet,
     * or if the connection attempt is successful (use {@link #isDone()} to
     * distinguish between the two)
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
     * Cancels the connection attempt and notifies all threads waiting for
     * this future.
     */
    void cancel();
}

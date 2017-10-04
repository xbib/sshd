package org.xbib.io.sshd.client.future;

import org.xbib.io.sshd.common.future.SshFuture;
import org.xbib.io.sshd.common.future.VerifiableFuture;

/**
 * An {@link SshFuture} for asynchronous authentication requests.
 */
public interface AuthFuture extends SshFuture<AuthFuture>, VerifiableFuture<AuthFuture> {
    /**
     * Returns the cause of the authentication failure.
     *
     * @return <code>null</code> if the authentication operation is not finished yet,
     * or if the connection attempt is successful (use {@link #isDone()} to distinguish
     * between the two).
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
     * @return <code>true</code> if the authentication operation is finished successfully.
     * <B>Note:</B> calling this method while the operation is in progress
     * returns {@code false}. Should check {@link #isDone()} in order to
     * ensure that the result is valid.
     */
    boolean isSuccess();

    /**
     * @return <code>false</code> if the authentication operation failed.
     * <B>Note:</B> the operation is considered failed if an exception
     * is received instead of a success/fail response code or the operation
     * is in progress. Should check {@link #isDone()} in order to
     * ensure that the result is valid.
     */
    boolean isFailure();

    /**
     * @return {@code true} if the connect operation has been canceled by
     * {@link #cancel()} method.
     */
    boolean isCanceled();

    /**
     * Notifies that the session has been authenticated.
     * This method is invoked by SSHD internally.  Please do not
     * call this method directly.
     *
     * @param authed Authentication success state
     */
    void setAuthed(boolean authed);

    /**
     * Cancels the authentication attempt and notifies all threads waiting for
     * this future.
     */
    void cancel();
}

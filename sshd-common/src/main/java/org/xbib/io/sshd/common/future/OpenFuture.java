package org.xbib.io.sshd.common.future;

/**
 * An {@link SshFuture} for asynchronous channel opening requests.
 */
public interface OpenFuture extends SshFuture<OpenFuture>, VerifiableFuture<OpenFuture> {
    /**
     * Returns the cause of the connection failure.
     *
     * @return <code>null</code> if the connect operation is not finished yet,
     * or if the connection attempt is successful (use {@link #isDone()} to
     * distinguish between the two).
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
     * @return <code>true</code> if the connect operation is finished successfully.
     */
    boolean isOpened();

    /**
     * @return {@code true} if the connect operation has been canceled by
     * {@link #cancel()} method.
     */
    boolean isCanceled();

    /**
     * Sets the newly connected session and notifies all threads waiting for
     * this future.  This method is invoked by SSHD internally.  Please do not
     * call this method directly.
     */
    void setOpened();

    /**
     * Cancels the connection attempt and notifies all threads waiting for
     * this future.
     */
    void cancel();

}

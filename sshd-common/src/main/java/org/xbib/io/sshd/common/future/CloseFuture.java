package org.xbib.io.sshd.common.future;

/**
 * An {@link org.xbib.io.sshd.common.future.SshFuture} for asynchronous close requests.
 */
public interface CloseFuture extends SshFuture<CloseFuture> {

    /**
     * @return {@code true} if the close request is finished and the target is closed.
     */
    boolean isClosed();

    /**
     * Marks this future as closed and notifies all threads waiting for this
     * future.  This method is invoked by SSHD internally.  Please do not call
     * this method directly.
     */
    void setClosed();

}

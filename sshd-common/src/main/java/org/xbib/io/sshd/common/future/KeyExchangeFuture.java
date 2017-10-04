package org.xbib.io.sshd.common.future;

/**
 *
 */
public interface KeyExchangeFuture extends SshFuture<KeyExchangeFuture>, VerifiableFuture<KeyExchangeFuture> {
    /**
     * Returns the cause of the exchange failure.
     *
     * @return <code>null</code> if the exchange operation is not finished yet,
     * or if the connection attempt is successful (use {@link #isDone()} to
     * distinguish between the two).
     */
    Throwable getException();
}

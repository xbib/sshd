package org.xbib.io.sshd.common.io;

import org.xbib.io.sshd.common.future.SshFuture;
import org.xbib.io.sshd.common.future.VerifiableFuture;

/**
 *
 */
public interface IoWriteFuture extends SshFuture<IoWriteFuture>, VerifiableFuture<IoWriteFuture> {
    /**
     * @return {@code true} if the write operation is finished successfully.
     */
    boolean isWritten();

    /**
     * @return the cause of the write failure if and only if the write
     * operation has failed due to an {@link Exception}.  Otherwise,
     * {@code null} is returned (use {@link #isDone()} to distinguish
     * between the two.
     */
    Throwable getException();

}

package org.xbib.io.sshd.common.io;

import org.xbib.io.sshd.common.future.SshFuture;
import org.xbib.io.sshd.common.future.VerifiableFuture;
import org.xbib.io.sshd.common.util.buffer.Buffer;

/**
 *
 */
public interface IoReadFuture extends SshFuture<IoReadFuture>, VerifiableFuture<IoReadFuture> {

    Buffer getBuffer();

    int getRead();

    /**
     * Returns the cause of the read failure.
     *
     * @return <code>null</code> if the read operation is not finished yet,
     * or if the read attempt is successful (use {@link #isDone()} to
     * distinguish between the two).
     */
    Throwable getException();
}

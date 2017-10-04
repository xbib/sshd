package org.xbib.io.sshd.common.io;

import org.xbib.io.sshd.common.Closeable;
import org.xbib.io.sshd.common.util.buffer.Buffer;

/**
 *
 */
public interface IoOutputStream extends Closeable {

    /**
     * <B>NOTE:</B> the buffer must not be touched until the returned write future is completed.
     *
     * @param buffer the {@link Buffer} to use
     * @return The {@link IoWriteFuture} for the operation
     */
    IoWriteFuture write(Buffer buffer);

}

package org.xbib.io.sshd.common.io;

import org.xbib.io.sshd.common.Closeable;
import org.xbib.io.sshd.common.util.buffer.Buffer;

/**
 *
 */
public interface IoInputStream extends Closeable {

    /**
     * NOTE: the buffer must not be touched until the returned read future is completed.
     *
     * @param buffer the {@link Buffer} to use
     * @return The {@link IoReadFuture} for the operation
     */
    IoReadFuture read(Buffer buffer);

}

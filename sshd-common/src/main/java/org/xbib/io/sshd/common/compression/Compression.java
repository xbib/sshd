package org.xbib.io.sshd.common.compression;

import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.io.IOException;

/**
 * Interface used to compress the stream of data between the
 * SSH server and clients.
 */
public interface Compression extends CompressionInformation {

    /**
     * Initialize this object to either compress or uncompress data.
     * This method must be called prior to any calls to either
     * <code>compress</code> or <code>uncompress</code>.
     * Once the object has been initialized, only one of
     * <code>compress</code> or <code>uncompress</code> methods can be
     * called.
     *
     * @param type  compression type
     * @param level compression level
     */
    void init(Type type, int level);

    /**
     * Compress the given buffer in place.
     *
     * @param buffer the buffer containing the data to compress
     * @throws IOException if an error occurs
     */
    void compress(Buffer buffer) throws IOException;

    /**
     * Uncompress the data in a buffer into another buffer.
     *
     * @param from the buffer containing the data to uncompress
     * @param to   the buffer receiving the uncompressed data
     * @throws IOException if an error occurs
     */
    void uncompress(Buffer from, Buffer to) throws IOException;

    /**
     * Enum identifying if this object will be used to compress
     * or uncompress data.
     */
    enum Type {
        Inflater,
        Deflater
    }
}

package org.xbib.io.sshd.common.util;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 *
 */
public interface Readable {

    /**
     * Wrap a {@link ByteBuffer} as a {@link Readable} instance
     *
     * @param buffer The {@link ByteBuffer} to wrap - never {@code null}
     * @return The {@link Readable} wrapper
     */
    static Readable readable(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "No buffer to wrap");
        return new Readable() {
            @Override
            public int available() {
                return buffer.remaining();
            }

            @Override
            public void getRawBytes(byte[] data, int offset, int len) {
                buffer.get(data, offset, len);
            }
        };
    }

    int available();

    void getRawBytes(byte[] data, int offset, int len);
}

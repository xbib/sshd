package org.xbib.io.sshd.common.random;

import org.xbib.io.sshd.common.NamedResource;

/**
 * A pseudo random number generator.
 */
public interface Random extends NamedResource {
    /**
     * Fill the buffer with random values
     *
     * @param bytes The bytes to fill
     * @see #fill(byte[], int, int)
     */
    default void fill(byte[] bytes) {
        fill(bytes, 0, bytes.length);
    }

    /**
     * Fill part of bytes with random values.
     *
     * @param bytes byte array to be filled.
     * @param start index to start filling at.
     * @param len   length of segment to fill.
     */
    void fill(byte[] bytes, int start, int len);

    /**
     * Returns a pseudo-random uniformly distributed {@code int}
     * in the half-open range [0, n).
     *
     * @param n The range upper limit
     * @return The randomly selected value in the range
     */
    int random(int n);
}

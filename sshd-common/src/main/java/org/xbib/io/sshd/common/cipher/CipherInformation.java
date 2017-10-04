package org.xbib.io.sshd.common.cipher;

/**
 *
 */
public interface CipherInformation {
    /**
     * @return The cipher's algorithm
     */
    String getAlgorithm();

    /**
     * @return The actual transformation used - e.g., AES/CBC/NoPadding
     */
    String getTransformation();

    /**
     * @return Size of the initialization vector (in bytes)
     */
    int getIVSize();

    /**
     * @return The block size (in bytes) for this cipher
     */
    int getBlockSize();
}

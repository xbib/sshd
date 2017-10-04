package org.xbib.io.sshd.common.digest;

/**
 *
 */
public interface DigestInformation {
    /**
     * @return The digest algorithm name
     */
    String getAlgorithm();

    /**
     * @return The number of bytes in the digest's output
     */
    int getBlockSize();

}

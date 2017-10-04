package org.xbib.io.sshd.common.mac;

/**
 *
 */
public interface MacInformation {
    /**
     * @return MAC algorithm name
     */
    String getAlgorithm();

    /**
     * @return MAC output block size in bytes - may be less than the default
     * - e.g., MD5-96
     */
    int getBlockSize();

    /**
     * @return The &quot;natural&quot; MAC block size in bytes
     */
    int getDefaultBlockSize();
}

package org.xbib.io.sshd.common.digest;

/**
 * Interface used to compute digests, based on algorithms such as MD5 or SHA1.
 * The digest implementation are compared first by the algorithm name (case
 * <U>insensitive</U> and second according to the block size
 */
public interface Digest extends DigestInformation, Comparable<Digest> {
    void init() throws Exception;

    void update(byte[] data) throws Exception;

    void update(byte[] data, int start, int len) throws Exception;

    byte[] digest() throws Exception;
}

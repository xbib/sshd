package org.xbib.io.sshd.common.digest;

import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.NumberUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.security.SecurityUtils;

import java.security.MessageDigest;
import java.util.Objects;

/**
 * Base class for Digest algorithms based on the JCE provider.
 */
public class BaseDigest implements Digest {

    private final String algorithm;
    private final int bsize;
    private int h;
    private String s;
    private MessageDigest md;

    /**
     * Create a new digest using the given algorithm and block size.
     * The initialization and creation of the underlying {@link MessageDigest}
     * object will be done in the {@link #init()} method.
     *
     * @param algorithm the JCE algorithm to use for this digest
     * @param bsize     the block size of this digest
     */
    public BaseDigest(String algorithm, int bsize) {
        this.algorithm = ValidateUtils.checkNotNullAndNotEmpty(algorithm, "No algorithm");
        ValidateUtils.checkTrue(bsize > 0, "Invalid block size: %d", bsize);
        this.bsize = bsize;
    }

    @Override
    public final String getAlgorithm() {
        return algorithm;
    }

    @Override
    public int getBlockSize() {
        return bsize;
    }

    @Override
    public void init() throws Exception {
        this.md = SecurityUtils.getMessageDigest(getAlgorithm());
    }

    @Override
    public void update(byte[] data) throws Exception {
        update(data, 0, NumberUtils.length(data));
    }

    @Override
    public void update(byte[] data, int start, int len) throws Exception {
        Objects.requireNonNull(md, "Digest not initialized").update(data, start, len);
    }

    /**
     * @return The current {@link MessageDigest} - may be {@code null} if {@link #init()} not called
     */
    protected MessageDigest getMessageDigest() {
        return md;
    }

    @Override
    public byte[] digest() throws Exception {
        return Objects.requireNonNull(md, "Digest not initialized").digest();
    }

    @Override
    public int hashCode() {
        synchronized (this) {
            if (h == 0) {
                h = Objects.hashCode(getAlgorithm()) + getBlockSize();
                if (h == 0) {
                    h = 1;
                }
            }
        }

        return h;
    }


    @Override
    public int compareTo(Digest that) {
        if (that == null) {
            return -1;    // push null(s) to end
        } else if (this == that) {
            return 0;
        }

        String thisAlg = getAlgorithm();
        String thatAlg = that.getAlgorithm();
        int nRes = GenericUtils.safeCompare(thisAlg, thatAlg, false);
        if (nRes != 0) {
            return nRes;    // debug breakpoint
        }

        nRes = Integer.compare(this.getBlockSize(), that.getBlockSize());
        if (nRes != 0) {
            return nRes;    // debug breakpoint
        }

        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        int nRes = compareTo((Digest) obj);
        return nRes == 0;
    }

    @Override
    public String toString() {
        synchronized (this) {
            if (s == null) {
                s = getClass().getSimpleName() + "[" + getAlgorithm() + ":" + getBlockSize() + "]";
            }
        }

        return s;
    }
}

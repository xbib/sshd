package org.xbib.io.sshd.common.mac;

import org.xbib.io.sshd.common.util.security.SecurityUtils;

import javax.crypto.spec.SecretKeySpec;

/**
 * Base class for <code>Mac</code> implementations based on the JCE provider.
 */
public class BaseMac implements Mac {

    private final String algorithm;
    private final int defbsize;
    private final int bsize;
    private final byte[] tmp;
    private javax.crypto.Mac mac;
    private String s;

    public BaseMac(String algorithm, int bsize, int defbsize) {
        this.algorithm = algorithm;
        this.bsize = bsize;
        this.defbsize = defbsize;
        this.tmp = new byte[defbsize];
    }

    @Override
    public final String getAlgorithm() {
        return algorithm;
    }

    @Override
    public final int getBlockSize() {
        return bsize;
    }

    @Override
    public final int getDefaultBlockSize() {
        return defbsize;
    }

    @Override
    public void init(byte[] key) throws Exception {
        if (key.length > defbsize) {
            byte[] tmp = new byte[defbsize];
            System.arraycopy(key, 0, tmp, 0, defbsize);
            key = tmp;
        }

        SecretKeySpec skey = new SecretKeySpec(key, algorithm);
        mac = SecurityUtils.getMac(algorithm);
        mac.init(skey);
    }

    @Override
    public void updateUInt(long i) {
        tmp[0] = (byte) (i >>> 24);
        tmp[1] = (byte) (i >>> 16);
        tmp[2] = (byte) (i >>> 8);
        tmp[3] = (byte) i;
        update(tmp, 0, 4);
    }

    @Override
    public void update(byte buf[], int offset, int len) {
        mac.update(buf, offset, len);
    }

    @Override
    public void doFinal(byte[] buf, int offset) throws Exception {
        int blockSize = getBlockSize();
        int defaultSize = getDefaultBlockSize();
        if (blockSize != defaultSize) {
            mac.doFinal(tmp, 0);
            System.arraycopy(tmp, 0, buf, offset, blockSize);
        } else {
            mac.doFinal(buf, offset);
        }
    }

    @Override
    public String toString() {
        synchronized (this) {
            if (s == null) {
                s = getClass().getSimpleName() + "[" + getAlgorithm() + "] - "
                        + " block=" + getBlockSize() + "/" + getDefaultBlockSize() + " bytes";
            }
        }

        return s;
    }
}

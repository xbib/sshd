package org.xbib.io.sshd.common.cipher;

/**
 * Represents a no-op cipher.
 * This cipher can not really be used during authentication and should only
 * be used after, so that authentication remains secured, but not the remaining
 * of the exchanges.
 */
public class CipherNone implements Cipher {
    public CipherNone() {
        super();
    }

    @Override
    public String getAlgorithm() {
        return "none";
    }

    @Override
    public String getTransformation() {
        return "none";
    }

    @Override
    public int getIVSize() {
        return 8;   // dummy
    }

    @Override
    public int getBlockSize() {
        return 16;  // dummy
    }

    @Override
    public void init(Mode mode, byte[] bytes, byte[] bytes1) throws Exception {
        // ignored - always succeeds
    }

    @Override
    public void update(byte[] input, int inputOffset, int inputLen) throws Exception {
        // ignored - always succeeds
    }
}

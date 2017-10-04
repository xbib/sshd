package org.xbib.io.sshd.common.cipher;

import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.security.SecurityUtils;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Base class for all Cipher implementations delegating to the JCE provider.
 */
public class BaseCipher implements Cipher {

    private final int ivsize;
    private final int bsize;
    private final String algorithm;
    private final String transformation;
    protected javax.crypto.Cipher cipher;
    private String s;

    public BaseCipher(int ivsize, int bsize, String algorithm, String transformation) {
        this.ivsize = ivsize;
        this.bsize = bsize;
        this.algorithm = ValidateUtils.checkNotNullAndNotEmpty(algorithm, "No algorithm");
        this.transformation = ValidateUtils.checkNotNullAndNotEmpty(transformation, "No transformation");
    }

    protected static byte[] resize(byte[] data, int size) {
        if (data.length > size) {
            byte[] tmp = new byte[size];
            System.arraycopy(data, 0, tmp, 0, size);
            data = tmp;
        }
        return data;
    }

    @Override
    public String getAlgorithm() {
        return algorithm;
    }

    @Override
    public String getTransformation() {
        return transformation;
    }

    @Override
    public int getIVSize() {
        return ivsize;
    }

    @Override
    public int getBlockSize() {
        return bsize;
    }

    @Override
    public void init(Mode mode, byte[] key, byte[] iv) throws Exception {
        key = resize(key, getBlockSize());
        iv = resize(iv, getIVSize());
        try {
            cipher = SecurityUtils.getCipher(getTransformation());
            cipher.init(Mode.Encrypt.equals(mode) ? javax.crypto.Cipher.ENCRYPT_MODE : javax.crypto.Cipher.DECRYPT_MODE,
                    new SecretKeySpec(key, getAlgorithm()),
                    new IvParameterSpec(iv));
        } catch (Exception e) {
            cipher = null;
            throw new SshException("Unable to initialize cipher " + this, e);
        }
    }

    @Override
    public void update(byte[] input, int inputOffset, int inputLen) throws Exception {
        cipher.update(input, inputOffset, inputLen, input, inputOffset);
    }

    @Override
    public String toString() {
        synchronized (this) {
            if (s == null) {
                s = getClass().getSimpleName()
                        + "[" + getAlgorithm()
                        + "," + getIVSize()
                        + "," + getBlockSize()
                        + "," + getTransformation()
                        + "]";
            }
        }

        return s;
    }
}

package org.xbib.io.sshd.common.cipher;

import org.xbib.io.sshd.common.util.security.SecurityUtils;

import javax.crypto.spec.SecretKeySpec;

/**
 *
 */
public class BaseRC4Cipher extends BaseCipher {

    public static final int SKIP_SIZE = 1536;

    public BaseRC4Cipher(int ivsize, int bsize) {
        super(ivsize, bsize, "ARCFOUR", "RC4");
    }

    @Override
    public void init(Mode mode, byte[] key, byte[] iv) throws Exception {
        key = resize(key, getBlockSize());
        try {
            cipher = SecurityUtils.getCipher(getTransformation());
            cipher.init(Mode.Encrypt.equals(mode) ? javax.crypto.Cipher.ENCRYPT_MODE : javax.crypto.Cipher.DECRYPT_MODE,
                    new SecretKeySpec(key, getAlgorithm()));

            byte[] foo = new byte[1];
            for (int i = 0; i < SKIP_SIZE; i++) {
                cipher.update(foo, 0, 1, foo, 0);
            }
        } catch (Exception e) {
            cipher = null;
            throw e;
        }
    }

}

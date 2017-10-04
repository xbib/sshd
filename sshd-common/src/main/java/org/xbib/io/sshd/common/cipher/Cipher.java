package org.xbib.io.sshd.common.cipher;

import org.xbib.io.sshd.common.util.NumberUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;

/**
 * Wrapper for a cryptographic cipher, used either for encryption
 * or decryption.
 */
public interface Cipher extends CipherInformation {

    /**
     * @param xform     The full cipher transformation - e.g., AES/CBC/NoPadding -
     *                  never {@code null}/empty
     * @param keyLength The required key length in bits - always positive
     * @return {@code true} if the cipher transformation <U>and</U> required
     * key length are supported
     * @see javax.crypto.Cipher#getMaxAllowedKeyLength(String)
     */
    static boolean checkSupported(String xform, int keyLength) {
        ValidateUtils.checkNotNullAndNotEmpty(xform, "No transformation");
        if (keyLength <= 0) {
            throw new IllegalArgumentException("Bad key length (" + keyLength + ") for cipher=" + xform);
        }

        try {
            int maxKeyLength = javax.crypto.Cipher.getMaxAllowedKeyLength(xform);
            return maxKeyLength >= keyLength;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Initialize the cipher for encryption or decryption with
     * the given key and initialization vector
     *
     * @param mode Encrypt/Decrypt initialization
     * @param key  Key bytes
     * @param iv   Initialization vector bytes
     * @throws Exception If failed to initialize
     */
    void init(Mode mode, byte[] key, byte[] iv) throws Exception;

    /**
     * Performs in-place encryption or decryption on the given data.
     *
     * @param input The input/output bytes
     * @throws Exception If failed to execute
     * @see #update(byte[], int, int)
     */
    default void update(byte[] input) throws Exception {
        update(input, 0, NumberUtils.length(input));
    }

    /**
     * Performs in-place encryption or decryption on the given data.
     *
     * @param input       The input/output bytes
     * @param inputOffset The offset of the data in the data buffer
     * @param inputLen    The number of bytes to update - starting at the given offset
     * @throws Exception If failed to execute
     */
    void update(byte[] input, int inputOffset, int inputLen) throws Exception;

    enum Mode {
        Encrypt, Decrypt
    }
}

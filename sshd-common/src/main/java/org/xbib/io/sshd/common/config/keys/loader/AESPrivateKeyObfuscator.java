package org.xbib.io.sshd.common.config.keys.loader;

import org.xbib.io.sshd.common.util.security.SecurityUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class AESPrivateKeyObfuscator extends AbstractPrivateKeyObfuscator {
    public static final String CIPHER_NAME = "AES";
    public static final AESPrivateKeyObfuscator INSTANCE = new AESPrivateKeyObfuscator();

    public AESPrivateKeyObfuscator() {
        super(CIPHER_NAME);
    }

    /**
     * @return A {@link List} of {@link Integer}s holding the available key
     * lengths values (in bits) for the JVM. <B>Note:</B> AES 256 requires
     * special JCE policy extension installation (e.g., for Java 7 see
     * <A HREF="http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html">this link</A>)
     */
    @SuppressWarnings("synthetic-access")
    public static List<Integer> getAvailableKeyLengths() {
        return LazyValuesHolder.KEY_LENGTHS;
    }

    @Override
    public List<Integer> getSupportedKeySizes() {
        return getAvailableKeyLengths();
    }

    @Override
    public byte[] applyPrivateKeyCipher(byte[] bytes, PrivateKeyEncryptionContext encContext, boolean encryptIt) throws GeneralSecurityException {
        int keyLength = resolveKeyLength(encContext);
        byte[] keyValue = deriveEncryptionKey(encContext, keyLength / Byte.SIZE);
        return applyPrivateKeyCipher(bytes, encContext, keyLength, keyValue, encryptIt);
    }

    @Override
    protected int resolveKeyLength(PrivateKeyEncryptionContext encContext) throws GeneralSecurityException {
        String cipherType = encContext.getCipherType();
        try {
            int keyLength = Integer.parseInt(cipherType);
            List<Integer> sizes = getSupportedKeySizes();
            for (Integer s : sizes) {
                if (s.intValue() == keyLength) {
                    return keyLength;
                }
            }

            throw new InvalidKeySpecException("Unknown " + getCipherName() + " key length: " + cipherType + " - supported: " + sizes);
        } catch (NumberFormatException e) {
            throw new InvalidKeySpecException("Bad " + getCipherName() + " key length (" + cipherType + "): " + e.getMessage(), e);
        }
    }

    private static class LazyValuesHolder {
        private static final List<Integer> KEY_LENGTHS =
                Collections.unmodifiableList(detectSupportedKeySizes());

        // AES 256 requires special JCE policy extension installation
        private static List<Integer> detectSupportedKeySizes() {
            List<Integer> sizes = new ArrayList<>();
            for (int keyLength = 128; keyLength < Short.MAX_VALUE /* just so it doesn't go forever */; keyLength += 64) {
                try {
                    byte[] keyAsBytes = new byte[keyLength / Byte.SIZE];
                    Key key = new SecretKeySpec(keyAsBytes, CIPHER_NAME);
                    Cipher c = SecurityUtils.getCipher(CIPHER_NAME);
                    c.init(Cipher.DECRYPT_MODE, key);
                    sizes.add(keyLength);
                } catch (GeneralSecurityException e) {
                    return sizes;
                }
            }

            throw new IllegalStateException("No limit encountered: " + sizes);
        }
    }
}

package org.xbib.io.sshd.common.config.keys.loader;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 */
public interface PrivateKeyObfuscator {
    /**
     * @return Basic cipher used to obfuscate
     */
    String getCipherName();

    /**
     * @return A {@link List} of the supported key sizes - <B>Note:</B> every
     * call returns a and <U>un-modifiable</U> instance.
     */
    List<Integer> getSupportedKeySizes();

    /**
     * @param <A>        Appendable generic type
     * @param sb         The {@link Appendable} instance to update
     * @param encContext context
     * @return Same appendable instance
     * @throws IOException I/O exception
     */
    <A extends Appendable> A appendPrivateKeyEncryptionContext(A sb, PrivateKeyEncryptionContext encContext) throws IOException;

    /**
     * @param encContext The encryption context
     * @return An initialization vector suitable to the specified context
     * @throws GeneralSecurityException security exception
     */
    byte[] generateInitializationVector(PrivateKeyEncryptionContext encContext) throws GeneralSecurityException;

    /**
     * @param bytes      Original bytes
     * @param encContext The encryption context
     * @param encryptIt  If {@code true} then encrypt the original bytes, otherwise decrypt them
     * @return The result of applying the cipher to the original bytes
     * @throws GeneralSecurityException If cannot encrypt/decrypt
     */
    byte[] applyPrivateKeyCipher(byte[] bytes, PrivateKeyEncryptionContext encContext, boolean encryptIt) throws GeneralSecurityException;
}

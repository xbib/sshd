package org.xbib.io.sshd.common.config.keys.loader;

import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class DESPrivateKeyObfuscator extends AbstractPrivateKeyObfuscator {
    public static final int DEFAULT_KEY_LENGTH = 24 /* hardwired size for 3DES */;
    public static final List<Integer> AVAILABLE_KEY_LENGTHS =
            Collections.unmodifiableList(Collections.singletonList(Integer.valueOf(DEFAULT_KEY_LENGTH)));
    public static final DESPrivateKeyObfuscator INSTANCE = new DESPrivateKeyObfuscator();

    public DESPrivateKeyObfuscator() {
        super("DES");
    }

    public static final PrivateKeyEncryptionContext resolveEffectiveContext(PrivateKeyEncryptionContext encContext) {
        if (encContext == null) {
            return null;
        }

        String cipherName = encContext.getCipherName();
        String cipherType = encContext.getCipherType();
        PrivateKeyEncryptionContext effContext = encContext;
        if ("EDE3".equalsIgnoreCase(cipherType)) {
            cipherName += "ede";
            effContext = encContext.clone();
            effContext.setCipherName(cipherName);
        }

        return effContext;
    }

    @Override
    public byte[] applyPrivateKeyCipher(byte[] bytes, PrivateKeyEncryptionContext encContext, boolean encryptIt) throws GeneralSecurityException {
        PrivateKeyEncryptionContext effContext = resolveEffectiveContext(encContext);
        byte[] keyValue = deriveEncryptionKey(effContext, DEFAULT_KEY_LENGTH);
        return applyPrivateKeyCipher(bytes, effContext, keyValue.length * Byte.SIZE, keyValue, encryptIt);
    }

    @Override
    public List<Integer> getSupportedKeySizes() {
        return AVAILABLE_KEY_LENGTHS;
    }

    @Override
    protected int resolveKeyLength(PrivateKeyEncryptionContext encContext) throws GeneralSecurityException {
        return DEFAULT_KEY_LENGTH;
    }

    @Override
    protected byte[] generateInitializationVector(int keyLength) {
        return super.generateInitializationVector(8 * Byte.SIZE);
    }
}
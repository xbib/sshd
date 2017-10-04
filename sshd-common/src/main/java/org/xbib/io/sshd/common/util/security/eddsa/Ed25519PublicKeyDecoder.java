package org.xbib.io.sshd.common.util.security.eddsa;

import org.xbib.io.sshd.eddsa.EdDSAPrivateKey;
import org.xbib.io.sshd.eddsa.EdDSAPublicKey;
import org.xbib.io.sshd.eddsa.spec.EdDSAPrivateKeySpec;
import org.xbib.io.sshd.eddsa.spec.EdDSAPublicKeySpec;
import org.xbib.io.sshd.common.config.keys.KeyEntryResolver;
import org.xbib.io.sshd.common.config.keys.impl.AbstractPublicKeyEntryDecoder;
import org.xbib.io.sshd.common.keyprovider.KeyPairProvider;
import org.xbib.io.sshd.common.util.security.SecurityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.util.Collections;
import java.util.Objects;

/**
 *
 */
public final class Ed25519PublicKeyDecoder extends AbstractPublicKeyEntryDecoder<EdDSAPublicKey, EdDSAPrivateKey> {
    public static final Ed25519PublicKeyDecoder INSTANCE = new Ed25519PublicKeyDecoder();

    private Ed25519PublicKeyDecoder() {
        super(EdDSAPublicKey.class, EdDSAPrivateKey.class, Collections.unmodifiableList(Collections.singletonList(KeyPairProvider.SSH_ED25519)));
    }

    public static byte[] getSeedValue(EdDSAPublicKey key) {
        // a bit of reverse-engineering on the EdDSAPublicKeySpec
        return (key == null) ? null : key.getAbyte();
    }

    @Override
    public EdDSAPublicKey clonePublicKey(EdDSAPublicKey key) throws GeneralSecurityException {
        if (key == null) {
            return null;
        } else {
            return generatePublicKey(new EdDSAPublicKeySpec(key.getA(), key.getParams()));
        }
    }

    @Override
    public EdDSAPrivateKey clonePrivateKey(EdDSAPrivateKey key) throws GeneralSecurityException {
        if (key == null) {
            return null;
        } else {
            return generatePrivateKey(new EdDSAPrivateKeySpec(key.getSeed(), key.getParams()));
        }
    }

    @Override
    public KeyPairGenerator getKeyPairGenerator() throws GeneralSecurityException {
        return SecurityUtils.getKeyPairGenerator(SecurityUtils.EDDSA);
    }

    @Override
    public String encodePublicKey(OutputStream s, EdDSAPublicKey key) throws IOException {
        Objects.requireNonNull(key, "No public key provided");
        KeyEntryResolver.encodeString(s, KeyPairProvider.SSH_ED25519);
        byte[] seed = getSeedValue(key);
        KeyEntryResolver.writeRLEBytes(s, seed);
        return KeyPairProvider.SSH_ED25519;
    }

    @Override
    public KeyFactory getKeyFactoryInstance() throws GeneralSecurityException {
        return SecurityUtils.getKeyFactory(SecurityUtils.EDDSA);
    }

    @Override
    public EdDSAPublicKey decodePublicKey(String keyType, InputStream keyData) throws IOException, GeneralSecurityException {
        byte[] seed = KeyEntryResolver.readRLEBytes(keyData);
        return EdDSAPublicKey.class.cast(SecurityUtils.generateEDDSAPublicKey(keyType, seed));
    }
}
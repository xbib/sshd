package org.xbib.io.sshd.common.config.keys.impl;

import org.xbib.io.sshd.common.config.keys.KeyEntryResolver;
import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.keyprovider.KeyPairProvider;
import org.xbib.io.sshd.common.util.security.SecurityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Collections;
import java.util.Objects;

/**
 *
 */
public class RSAPublicKeyDecoder extends AbstractPublicKeyEntryDecoder<RSAPublicKey, RSAPrivateKey> {
    public static final RSAPublicKeyDecoder INSTANCE = new RSAPublicKeyDecoder();

    public RSAPublicKeyDecoder() {
        super(RSAPublicKey.class, RSAPrivateKey.class, Collections.unmodifiableList(Collections.singletonList(KeyPairProvider.SSH_RSA)));
    }

    @Override
    public RSAPublicKey decodePublicKey(String keyType, InputStream keyData) throws IOException, GeneralSecurityException {
        if (!KeyPairProvider.SSH_RSA.equals(keyType)) { // just in case we were invoked directly
            throw new InvalidKeySpecException("Unexpected key type: " + keyType);
        }

        BigInteger e = KeyEntryResolver.decodeBigInt(keyData);
        BigInteger n = KeyEntryResolver.decodeBigInt(keyData);

        return generatePublicKey(new RSAPublicKeySpec(n, e));
    }

    @Override
    public String encodePublicKey(OutputStream s, RSAPublicKey key) throws IOException {
        Objects.requireNonNull(key, "No public key provided");
        KeyEntryResolver.encodeString(s, KeyPairProvider.SSH_RSA);
        KeyEntryResolver.encodeBigInt(s, key.getPublicExponent());
        KeyEntryResolver.encodeBigInt(s, key.getModulus());

        return KeyPairProvider.SSH_RSA;
    }

    @Override
    public RSAPublicKey clonePublicKey(RSAPublicKey key) throws GeneralSecurityException {
        if (key == null) {
            return null;
        } else {
            return generatePublicKey(new RSAPublicKeySpec(key.getModulus(), key.getPublicExponent()));
        }
    }

    @Override
    public RSAPrivateKey clonePrivateKey(RSAPrivateKey key) throws GeneralSecurityException {
        if (key == null) {
            return null;
        }

        if (!(key instanceof RSAPrivateCrtKey)) {
            throw new InvalidKeyException("Cannot clone a non-RSAPrivateCrtKey: " + key.getClass().getSimpleName());
        }

        RSAPrivateCrtKey rsaPrv = (RSAPrivateCrtKey) key;
        return generatePrivateKey(
                new RSAPrivateCrtKeySpec(
                        rsaPrv.getModulus(),
                        rsaPrv.getPublicExponent(),
                        rsaPrv.getPrivateExponent(),
                        rsaPrv.getPrimeP(),
                        rsaPrv.getPrimeQ(),
                        rsaPrv.getPrimeExponentP(),
                        rsaPrv.getPrimeExponentQ(),
                        rsaPrv.getCrtCoefficient()));
    }

    @Override
    public KeyPairGenerator getKeyPairGenerator() throws GeneralSecurityException {
        return SecurityUtils.getKeyPairGenerator(KeyUtils.RSA_ALGORITHM);
    }

    @Override
    public KeyFactory getKeyFactoryInstance() throws GeneralSecurityException {
        return SecurityUtils.getKeyFactory(KeyUtils.RSA_ALGORITHM);
    }
}

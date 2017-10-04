package org.xbib.io.sshd.common.config.keys.loader.openssh;

import org.xbib.io.sshd.common.config.keys.FilePasswordProvider;
import org.xbib.io.sshd.common.config.keys.KeyEntryResolver;
import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.config.keys.impl.AbstractPrivateKeyEntryDecoder;
import org.xbib.io.sshd.common.keyprovider.KeyPairProvider;
import org.xbib.io.sshd.common.util.security.SecurityUtils;

import java.io.IOException;
import java.io.InputStream;
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
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Collections;
import java.util.Objects;

/**
 *
 */
public class OpenSSHRSAPrivateKeyDecoder extends AbstractPrivateKeyEntryDecoder<RSAPublicKey, RSAPrivateKey> {
    public static final BigInteger DEFAULT_PUBLIC_EXPONENT = new BigInteger("65537");
    public static final OpenSSHRSAPrivateKeyDecoder INSTANCE = new OpenSSHRSAPrivateKeyDecoder();

    public OpenSSHRSAPrivateKeyDecoder() {
        super(RSAPublicKey.class, RSAPrivateKey.class, Collections.unmodifiableList(Collections.singletonList(KeyPairProvider.SSH_RSA)));
    }

    @Override
    public RSAPrivateKey decodePrivateKey(String keyType, FilePasswordProvider passwordProvider, InputStream keyData)
            throws IOException, GeneralSecurityException {
        if (!KeyPairProvider.SSH_RSA.equals(keyType)) { // just in case we were invoked directly
            throw new InvalidKeySpecException("Unexpected key type: " + keyType);
        }

        BigInteger n = KeyEntryResolver.decodeBigInt(keyData);
        BigInteger e = KeyEntryResolver.decodeBigInt(keyData);
        if (!Objects.equals(e, DEFAULT_PUBLIC_EXPONENT)) {
        }

        BigInteger d = KeyEntryResolver.decodeBigInt(keyData);
        BigInteger inverseQmodP = KeyEntryResolver.decodeBigInt(keyData);
        Objects.requireNonNull(inverseQmodP, "Missing iqmodp"); // TODO run some validation on it
        BigInteger p = KeyEntryResolver.decodeBigInt(keyData);
        BigInteger q = KeyEntryResolver.decodeBigInt(keyData);
        BigInteger modulus = p.multiply(q);
        if (!Objects.equals(n, modulus)) {
        }

        return generatePrivateKey(new RSAPrivateKeySpec(n, d));
    }

    @Override
    public boolean isPublicKeyRecoverySupported() {
        return true;
    }

    @Override
    public RSAPublicKey recoverPublicKey(RSAPrivateKey privateKey) throws GeneralSecurityException {
        return KeyUtils.recoverRSAPublicKey(privateKey);
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

package org.xbib.io.sshd.common.config.keys.loader.openssh;

import org.xbib.io.sshd.common.cipher.ECCurves;
import org.xbib.io.sshd.common.config.keys.FilePasswordProvider;
import org.xbib.io.sshd.common.config.keys.KeyEntryResolver;
import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.config.keys.impl.AbstractPrivateKeyEntryDecoder;
import org.xbib.io.sshd.common.util.security.SecurityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchProviderException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;

/**
 *
 */
public class OpenSSHECDSAPrivateKeyEntryDecoder extends AbstractPrivateKeyEntryDecoder<ECPublicKey, ECPrivateKey> {
    public static final OpenSSHECDSAPrivateKeyEntryDecoder INSTANCE = new OpenSSHECDSAPrivateKeyEntryDecoder();

    public OpenSSHECDSAPrivateKeyEntryDecoder() {
        super(ECPublicKey.class, ECPrivateKey.class, ECCurves.KEY_TYPES);
    }

    @Override
    public ECPrivateKey decodePrivateKey(String keyType, FilePasswordProvider passwordProvider, InputStream keyData)
            throws IOException, GeneralSecurityException {
        ECCurves curve = ECCurves.fromKeyType(keyType);
        if (curve == null) {
            throw new InvalidKeySpecException("Not an EC curve name: " + keyType);
        }

        if (!SecurityUtils.isECCSupported()) {
            throw new NoSuchProviderException("ECC not supported");
        }

        String keyCurveName = curve.getName();
        // see rfc5656 section 3.1
        String encCurveName = KeyEntryResolver.decodeString(keyData);
        if (!keyCurveName.equals(encCurveName)) {
            throw new InvalidKeySpecException("Mismatched key curve name (" + keyCurveName + ") vs. encoded one (" + encCurveName + ")");
        }

        byte[] pubKey = KeyEntryResolver.readRLEBytes(keyData);
        Objects.requireNonNull(pubKey, "No public point");  // TODO validate it is a valid ECPoint
        BigInteger s = KeyEntryResolver.decodeBigInt(keyData);
        ECParameterSpec params = curve.getParameters();
        return generatePrivateKey(new ECPrivateKeySpec(s, params));
    }

    @Override
    public String encodePrivateKey(OutputStream s, ECPrivateKey key) throws IOException {
        Objects.requireNonNull(key, "No private key provided");
        return null;
    }

    @Override
    public ECPublicKey recoverPublicKey(ECPrivateKey prvKey) throws GeneralSecurityException {
        ECCurves curve = ECCurves.fromECKey(prvKey);
        if (curve == null) {
            throw new InvalidKeyException("Unknown curve");
        }
        // TODO see how we can figure out the public value
        return super.recoverPublicKey(prvKey);
    }

    @Override
    public ECPublicKey clonePublicKey(ECPublicKey key) throws GeneralSecurityException {
        if (!SecurityUtils.isECCSupported()) {
            throw new NoSuchProviderException("ECC not supported");
        }

        if (key == null) {
            return null;
        }

        ECParameterSpec params = key.getParams();
        if (params == null) {
            throw new InvalidKeyException("Missing parameters in key");
        }

        return generatePublicKey(new ECPublicKeySpec(key.getW(), params));
    }

    @Override
    public ECPrivateKey clonePrivateKey(ECPrivateKey key) throws GeneralSecurityException {
        if (!SecurityUtils.isECCSupported()) {
            throw new NoSuchProviderException("ECC not supported");
        }

        if (key == null) {
            return null;
        }

        ECParameterSpec params = key.getParams();
        if (params == null) {
            throw new InvalidKeyException("Missing parameters in key");
        }

        return generatePrivateKey(new ECPrivateKeySpec(key.getS(), params));
    }

    @Override
    public KeyFactory getKeyFactoryInstance() throws GeneralSecurityException {
        if (SecurityUtils.isECCSupported()) {
            return SecurityUtils.getKeyFactory(KeyUtils.EC_ALGORITHM);
        } else {
            throw new NoSuchProviderException("ECC not supported");
        }
    }

    @Override
    public KeyPair generateKeyPair(int keySize) throws GeneralSecurityException {
        ECCurves curve = ECCurves.fromCurveSize(keySize);
        if (curve == null) {
            throw new InvalidKeySpecException("Unknown curve for key size=" + keySize);
        }

        KeyPairGenerator gen = getKeyPairGenerator();
        gen.initialize(curve.getParameters());
        return gen.generateKeyPair();
    }

    @Override
    public KeyPairGenerator getKeyPairGenerator() throws GeneralSecurityException {
        if (SecurityUtils.isECCSupported()) {
            return SecurityUtils.getKeyPairGenerator(KeyUtils.EC_ALGORITHM);
        } else {
            throw new NoSuchProviderException("ECC not supported");
        }
    }
}

package org.xbib.io.sshd.common.config.keys.loader.openssh;

import org.xbib.io.sshd.common.config.keys.FilePasswordProvider;
import org.xbib.io.sshd.common.config.keys.KeyEntryResolver;
import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.config.keys.impl.AbstractPrivateKeyEntryDecoder;
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
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.Objects;

/**
 */
public class OpenSSHDSSPrivateKeyEntryDecoder extends AbstractPrivateKeyEntryDecoder<DSAPublicKey, DSAPrivateKey> {
    public static final OpenSSHDSSPrivateKeyEntryDecoder INSTANCE = new OpenSSHDSSPrivateKeyEntryDecoder();

    public OpenSSHDSSPrivateKeyEntryDecoder() {
        super(DSAPublicKey.class, DSAPrivateKey.class, Collections.unmodifiableList(Collections.singletonList(KeyPairProvider.SSH_DSS)));
    }

    @Override
    public DSAPrivateKey decodePrivateKey(String keyType, FilePasswordProvider passwordProvider, InputStream keyData)
            throws IOException, GeneralSecurityException {
        if (!KeyPairProvider.SSH_DSS.equals(keyType)) { // just in case we were invoked directly
            throw new InvalidKeySpecException("Unexpected key type: " + keyType);
        }

        BigInteger p = KeyEntryResolver.decodeBigInt(keyData);
        BigInteger q = KeyEntryResolver.decodeBigInt(keyData);
        BigInteger g = KeyEntryResolver.decodeBigInt(keyData);
        BigInteger y = KeyEntryResolver.decodeBigInt(keyData);
        Objects.requireNonNull(y, "No public key data");   // TODO run some validation on it
        BigInteger x = KeyEntryResolver.decodeBigInt(keyData);

        return generatePrivateKey(new DSAPrivateKeySpec(x, p, q, g));
    }

    @Override
    public String encodePrivateKey(OutputStream s, DSAPrivateKey key) throws IOException {
        Objects.requireNonNull(key, "No private key provided");

        DSAParams keyParams = Objects.requireNonNull(key.getParams(), "No DSA params available");
        BigInteger p = keyParams.getP();
        KeyEntryResolver.encodeBigInt(s, p);
        KeyEntryResolver.encodeBigInt(s, keyParams.getQ());

        BigInteger g = keyParams.getG();
        KeyEntryResolver.encodeBigInt(s, g);

        BigInteger x = key.getX();
        BigInteger y = g.modPow(x, p);
        KeyEntryResolver.encodeBigInt(s, y);
        KeyEntryResolver.encodeBigInt(s, x);
        return KeyPairProvider.SSH_DSS;
    }

    @Override
    public boolean isPublicKeyRecoverySupported() {
        return true;
    }

    @Override
    public DSAPublicKey recoverPublicKey(DSAPrivateKey privateKey) throws GeneralSecurityException {
        return KeyUtils.recoverDSAPublicKey(privateKey);
    }

    @Override
    public DSAPublicKey clonePublicKey(DSAPublicKey key) throws GeneralSecurityException {
        if (key == null) {
            return null;
        }

        DSAParams params = key.getParams();
        if (params == null) {
            throw new InvalidKeyException("Missing parameters in key");
        }

        return generatePublicKey(new DSAPublicKeySpec(key.getY(), params.getP(), params.getQ(), params.getG()));
    }

    @Override
    public DSAPrivateKey clonePrivateKey(DSAPrivateKey key) throws GeneralSecurityException {
        if (key == null) {
            return null;
        }

        DSAParams params = key.getParams();
        if (params == null) {
            throw new InvalidKeyException("Missing parameters in key");
        }

        return generatePrivateKey(new DSAPrivateKeySpec(key.getX(), params.getP(), params.getQ(), params.getG()));
    }

    @Override
    public KeyPairGenerator getKeyPairGenerator() throws GeneralSecurityException {
        return SecurityUtils.getKeyPairGenerator(KeyUtils.DSS_ALGORITHM);
    }

    @Override
    public KeyFactory getKeyFactoryInstance() throws GeneralSecurityException {
        return SecurityUtils.getKeyFactory(KeyUtils.DSS_ALGORITHM);
    }
}

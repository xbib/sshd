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
public class DSSPublicKeyEntryDecoder extends AbstractPublicKeyEntryDecoder<DSAPublicKey, DSAPrivateKey> {
    public static final DSSPublicKeyEntryDecoder INSTANCE = new DSSPublicKeyEntryDecoder();

    public DSSPublicKeyEntryDecoder() {
        super(DSAPublicKey.class, DSAPrivateKey.class, Collections.unmodifiableList(Collections.singletonList(KeyPairProvider.SSH_DSS)));
    }

    @Override
    public DSAPublicKey decodePublicKey(String keyType, InputStream keyData) throws IOException, GeneralSecurityException {
        if (!KeyPairProvider.SSH_DSS.equals(keyType)) { // just in case we were invoked directly
            throw new InvalidKeySpecException("Unexpected key type: " + keyType);
        }

        BigInteger p = KeyEntryResolver.decodeBigInt(keyData);
        BigInteger q = KeyEntryResolver.decodeBigInt(keyData);
        BigInteger g = KeyEntryResolver.decodeBigInt(keyData);
        BigInteger y = KeyEntryResolver.decodeBigInt(keyData);

        return generatePublicKey(new DSAPublicKeySpec(y, p, q, g));
    }

    @Override
    public String encodePublicKey(OutputStream s, DSAPublicKey key) throws IOException {
        Objects.requireNonNull(key, "No public key provided");

        DSAParams keyParams = Objects.requireNonNull(key.getParams(), "No DSA params available");
        KeyEntryResolver.encodeString(s, KeyPairProvider.SSH_DSS);
        KeyEntryResolver.encodeBigInt(s, keyParams.getP());
        KeyEntryResolver.encodeBigInt(s, keyParams.getQ());
        KeyEntryResolver.encodeBigInt(s, keyParams.getG());
        KeyEntryResolver.encodeBigInt(s, key.getY());

        return KeyPairProvider.SSH_DSS;
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

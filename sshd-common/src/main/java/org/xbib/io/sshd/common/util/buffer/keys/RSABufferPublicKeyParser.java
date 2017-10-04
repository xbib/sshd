package org.xbib.io.sshd.common.util.buffer.keys;

import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.keyprovider.KeyPairProvider;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;

/**
 *
 */
public class RSABufferPublicKeyParser extends AbstractBufferPublicKeyParser<RSAPublicKey> {
    public static final RSABufferPublicKeyParser INSTANCE = new RSABufferPublicKeyParser();

    public RSABufferPublicKeyParser() {
        super(RSAPublicKey.class, KeyPairProvider.SSH_RSA);
    }

    @Override
    public RSAPublicKey getRawPublicKey(String keyType, Buffer buffer) throws GeneralSecurityException {
        ValidateUtils.checkTrue(isKeyTypeSupported(keyType), "Unsupported key type: %s", keyType);
        BigInteger e = buffer.getMPInt();
        BigInteger n = buffer.getMPInt();
        return generatePublicKey(KeyUtils.RSA_ALGORITHM, new RSAPublicKeySpec(n, e));
    }
}

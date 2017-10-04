package org.xbib.io.sshd.common.util.buffer.keys;

import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.keyprovider.KeyPairProvider;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.DSAPublicKeySpec;

/**
 *
 */
public class DSSBufferPublicKeyParser extends AbstractBufferPublicKeyParser<DSAPublicKey> {
    public static final DSSBufferPublicKeyParser INSTANCE = new DSSBufferPublicKeyParser();

    public DSSBufferPublicKeyParser() {
        super(DSAPublicKey.class, KeyPairProvider.SSH_DSS);
    }

    @Override
    public DSAPublicKey getRawPublicKey(String keyType, Buffer buffer) throws GeneralSecurityException {
        ValidateUtils.checkTrue(isKeyTypeSupported(keyType), "Unsupported key type: %s", keyType);
        BigInteger p = buffer.getMPInt();
        BigInteger q = buffer.getMPInt();
        BigInteger g = buffer.getMPInt();
        BigInteger y = buffer.getMPInt();

        return generatePublicKey(KeyUtils.DSS_ALGORITHM, new DSAPublicKeySpec(y, p, q, g));
    }
}

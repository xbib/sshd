package org.xbib.io.sshd.common.util.buffer.keys;

import org.xbib.io.sshd.common.keyprovider.KeyPairProvider;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.security.SecurityUtils;

import java.security.GeneralSecurityException;
import java.security.PublicKey;

/**
 *
 */
public class ED25519BufferPublicKeyParser extends AbstractBufferPublicKeyParser<PublicKey> {
    public static final ED25519BufferPublicKeyParser INSTANCE = new ED25519BufferPublicKeyParser();

    public ED25519BufferPublicKeyParser() {
        super(PublicKey.class, KeyPairProvider.SSH_ED25519);
    }

    @Override
    public PublicKey getRawPublicKey(String keyType, Buffer buffer) throws GeneralSecurityException {
        ValidateUtils.checkTrue(isKeyTypeSupported(keyType), "Unsupported key type: %s", keyType);
        byte[] seed = buffer.getBytes();
        return SecurityUtils.generateEDDSAPublicKey(keyType, seed);
    }
}

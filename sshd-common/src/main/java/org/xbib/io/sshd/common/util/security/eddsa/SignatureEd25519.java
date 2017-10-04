package org.xbib.io.sshd.common.util.security.eddsa;

import org.xbib.io.sshd.eddsa.EdDSAEngine;
import org.xbib.io.sshd.common.keyprovider.KeyPairProvider;
import org.xbib.io.sshd.common.signature.AbstractSignature;
import org.xbib.io.sshd.common.util.Pair;
import org.xbib.io.sshd.common.util.ValidateUtils;

/**
 *
 */
public class SignatureEd25519 extends AbstractSignature {
    public SignatureEd25519() {
        super(EdDSAEngine.SIGNATURE_ALGORITHM);
    }

    @Override
    public boolean verify(byte[] sig) throws Exception {
        byte[] data = sig;
        Pair<String, byte[]> encoding = extractEncodedSignature(data);
        if (encoding != null) {
            String keyType = encoding.getFirst();
            ValidateUtils.checkTrue(KeyPairProvider.SSH_ED25519.equals(keyType), "Mismatched key type: %s", keyType);
            data = encoding.getSecond();
        }

        return doVerify(data);
    }
}
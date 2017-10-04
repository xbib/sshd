package org.xbib.io.sshd.common.signature;

import org.xbib.io.sshd.common.keyprovider.KeyPairProvider;
import org.xbib.io.sshd.common.util.Pair;
import org.xbib.io.sshd.common.util.ValidateUtils;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.interfaces.RSAKey;

/**
 * RSA <code>Signature</code>.
 */
public class SignatureRSA extends AbstractSignature {
    public static final String DEFAULT_ALGORITHM = "SHA1withRSA";

    private int verifierSignatureSize = -1;

    public SignatureRSA() {
        super(DEFAULT_ALGORITHM);
    }

    protected SignatureRSA(String algorithm) {
        super(algorithm);
    }

    public static int getVerifierSignatureSize(RSAKey key) {
        BigInteger modulus = key.getModulus();
        return (modulus.bitLength() + Byte.SIZE - 1) / Byte.SIZE;
    }

    /**
     * @return The expected number of bytes in the signature - non-positive
     * if not initialized or not intended to be used for verification
     */
    protected int getVerifierSignatureSize() {
        return verifierSignatureSize;
    }

    @Override
    public void initVerifier(PublicKey key) throws Exception {
        super.initVerifier(key);
        RSAKey rsaKey = ValidateUtils.checkInstanceOf(key, RSAKey.class, "Not an RSA key");
        verifierSignatureSize = getVerifierSignatureSize(rsaKey);
    }

    @Override
    public boolean verify(byte[] sig) throws Exception {
        byte[] data = sig;
        Pair<String, byte[]> encoding = extractEncodedSignature(data);
        if (encoding != null) {
            String keyType = encoding.getFirst();
            ValidateUtils.checkTrue(KeyPairProvider.SSH_RSA.equals(keyType), "Mismatched key type: %s", keyType);
            data = encoding.getSecond();
        }

        int expectedSize = getVerifierSignatureSize();
        ValidateUtils.checkTrue(expectedSize > 0, "Signature verification size has not been initialized");
        // Pad with zero if value is trimmed
        if (data.length < expectedSize) {
            byte[] pad = new byte[expectedSize];
            System.arraycopy(data, 0, pad, pad.length - data.length, data.length);
            data = pad;
        }

        return doVerify(data);
    }
}

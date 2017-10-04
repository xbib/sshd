package org.xbib.io.sshd.common.signature;

import org.xbib.io.sshd.common.keyprovider.KeyPairProvider;
import org.xbib.io.sshd.common.util.NumberUtils;
import org.xbib.io.sshd.common.util.Pair;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.BufferUtils;
import org.xbib.io.sshd.common.util.io.der.DERParser;
import org.xbib.io.sshd.common.util.io.der.DERWriter;

import java.io.StreamCorruptedException;
import java.math.BigInteger;
import java.security.SignatureException;


/**
 * DSA <code>Signature</code>.
 */
public class SignatureDSA extends AbstractSignature {
    public static final String DEFAULT_ALGORITHM = "SHA1withDSA";

    public static final int DSA_SIGNATURE_LENGTH = 40;
    // result must be 40 bytes, but length of r and s may not exceed 20 bytes
    public static final int MAX_SIGNATURE_VALUE_LENGTH = DSA_SIGNATURE_LENGTH / 2;

    public SignatureDSA() {
        this(DEFAULT_ALGORITHM);
    }

    protected SignatureDSA(String algorithm) {
        super(algorithm);
    }

    public static void putBigInteger(BigInteger value, byte[] result, int offset) {
        byte[] data = value.toByteArray();
        boolean maxExceeded = data.length > MAX_SIGNATURE_VALUE_LENGTH;
        int dstOffset = maxExceeded ? 0 : (MAX_SIGNATURE_VALUE_LENGTH - data.length);
        System.arraycopy(data, maxExceeded ? 1 : 0,
                result, offset + dstOffset,
                Math.min(MAX_SIGNATURE_VALUE_LENGTH, data.length));
    }

    @Override
    public byte[] sign() throws Exception {
        byte[] sig = super.sign();

        try (DERParser parser = new DERParser(sig)) {
            int type = parser.read();
            if (type != 0x30) {
                throw new StreamCorruptedException("Invalid signature format - not a DER SEQUENCE: 0x" + Integer.toHexString(type));
            }

            // length of remaining encoding of the 2 integers
            int remainLen = parser.readLength();
            /*
             * There are supposed to be 2 INTEGERs, each encoded with:
             *
             *  - one byte representing the fact that it is an INTEGER
             *  - one byte of the integer encoding length
             *  - at least one byte of integer data (zero length is not an option)
             */
            if (remainLen < (2 * 3)) {
                throw new StreamCorruptedException("Invalid signature format - not enough encoded data length: " + remainLen);
            }

            BigInteger r = parser.readBigInteger();
            BigInteger s = parser.readBigInteger();

            byte[] result = new byte[DSA_SIGNATURE_LENGTH];
            putBigInteger(r, result, 0);
            putBigInteger(s, result, MAX_SIGNATURE_VALUE_LENGTH);
            return result;
        }
    }

    @Override
    public boolean verify(byte[] sig) throws Exception {
        int sigLen = NumberUtils.length(sig);
        byte[] data = sig;

        if (sigLen != DSA_SIGNATURE_LENGTH) {
            // probably some encoded data
            Pair<String, byte[]> encoding = extractEncodedSignature(sig);
            if (encoding != null) {
                String keyType = encoding.getFirst();
                ValidateUtils.checkTrue(KeyPairProvider.SSH_DSS.equals(keyType), "Mismatched key type: %s", keyType);
                data = encoding.getSecond();
                sigLen = NumberUtils.length(data);
            }
        }

        if (sigLen != DSA_SIGNATURE_LENGTH) {
            throw new SignatureException("Bad signature length (" + sigLen + " instead of " + DSA_SIGNATURE_LENGTH + ")"
                    + " for " + BufferUtils.toHex(':', data));
        }

        byte[] rEncoding;
        try (DERWriter w = new DERWriter(MAX_SIGNATURE_VALUE_LENGTH + 4)) {     // in case length > 0x7F
            w.writeBigInteger(data, 0, MAX_SIGNATURE_VALUE_LENGTH);
            rEncoding = w.toByteArray();
        }

        byte[] sEncoding;
        try (DERWriter w = new DERWriter(MAX_SIGNATURE_VALUE_LENGTH + 4)) {     // in case length > 0x7F
            w.writeBigInteger(data, MAX_SIGNATURE_VALUE_LENGTH, MAX_SIGNATURE_VALUE_LENGTH);
            sEncoding = w.toByteArray();
        }

        int length = rEncoding.length + sEncoding.length;
        byte[] encoded;
        try (DERWriter w = new DERWriter(1 + length + 4)) {  // in case length > 0x7F
            w.write(0x30); // SEQUENCE
            w.writeLength(length);
            w.write(rEncoding);
            w.write(sEncoding);
            encoded = w.toByteArray();
        }

        return doVerify(encoded);
    }
}

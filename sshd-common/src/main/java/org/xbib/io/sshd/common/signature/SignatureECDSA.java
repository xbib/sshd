package org.xbib.io.sshd.common.signature;

import org.xbib.io.sshd.common.cipher.ECCurves;
import org.xbib.io.sshd.common.util.Pair;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;
import org.xbib.io.sshd.common.util.io.der.DERParser;
import org.xbib.io.sshd.common.util.io.der.DERWriter;

import java.io.StreamCorruptedException;
import java.math.BigInteger;

/**
 * Signature algorithm for EC keys using ECDSA.
 */
public class SignatureECDSA extends AbstractSignature {
    protected SignatureECDSA(String algo) {
        super(algo);
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
            // Write the <r,s> to its own types writer.
            Buffer rsBuf = new ByteArrayBuffer();
            rsBuf.putMPInt(r);
            rsBuf.putMPInt(s);

            return rsBuf.getCompactData();
        }
    }

    @Override
    public boolean verify(byte[] sig) throws Exception {
        byte[] data = sig;
        Pair<String, byte[]> encoding = extractEncodedSignature(data);
        if (encoding != null) {
            String keyType = encoding.getFirst();
            ECCurves curve = ECCurves.fromKeyType(keyType);
            ValidateUtils.checkNotNull(curve, "Unknown curve type: %s", keyType);
            data = encoding.getSecond();
        }

        Buffer rsBuf = new ByteArrayBuffer(data);
        byte[] rArray = rsBuf.getMPIntAsBytes();
        byte[] rEncoding;
        try (DERWriter w = new DERWriter(rArray.length + 4)) {     // in case length > 0x7F
            w.writeBigInteger(rArray);
            rEncoding = w.toByteArray();
        }

        byte[] sArray = rsBuf.getMPIntAsBytes();
        byte[] sEncoding;
        try (DERWriter w = new DERWriter(sArray.length + 4)) {     // in case length > 0x7F
            w.writeBigInteger(sArray);
            sEncoding = w.toByteArray();
        }

        int remaining = rsBuf.available();
        if (remaining != 0) {
            throw new StreamCorruptedException("Signature had padding - remaining=" + remaining);
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

    public static class SignatureECDSA256 extends SignatureECDSA {
        public static final String DEFAULT_ALGORITHM = "SHA256withECDSA";

        public SignatureECDSA256() {
            super(DEFAULT_ALGORITHM);
        }
    }

    public static class SignatureECDSA384 extends SignatureECDSA {
        public static final String DEFAULT_ALGORITHM = "SHA384withECDSA";

        public SignatureECDSA384() {
            super(DEFAULT_ALGORITHM);
        }
    }

    public static class SignatureECDSA521 extends SignatureECDSA {
        public static final String DEFAULT_ALGORITHM = "SHA512withECDSA";

        public SignatureECDSA521() {
            super(DEFAULT_ALGORITHM);
        }
    }
}

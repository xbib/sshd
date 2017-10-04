package org.xbib.io.sshd.common.config.keys.loader.pem;

import org.xbib.io.sshd.common.config.keys.FilePasswordProvider;
import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.util.io.NoCloseInputStream;
import org.xbib.io.sshd.common.util.io.der.ASN1Object;
import org.xbib.io.sshd.common.util.io.der.ASN1Type;
import org.xbib.io.sshd.common.util.io.der.DERParser;
import org.xbib.io.sshd.common.util.security.SecurityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.DSAPublicKeySpec;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class DSSPEMResourceKeyPairParser extends AbstractPEMResourceKeyPairParser {
    // Not exactly according to standard but good enough
    public static final String BEGIN_MARKER = "BEGIN DSA PRIVATE KEY";
    public static final List<String> BEGINNERS =
            Collections.unmodifiableList(Collections.singletonList(BEGIN_MARKER));

    public static final String END_MARKER = "END DSA PRIVATE KEY";
    public static final List<String> ENDERS =
            Collections.unmodifiableList(Collections.singletonList(END_MARKER));

    /**
     * @see <A HREF="https://tools.ietf.org/html/rfc3279#section-2.3.2">RFC-3279 section 2.3.2</A>
     */
    public static final String DSS_OID = "1.2.840.10040.4.1";

    public static final DSSPEMResourceKeyPairParser INSTANCE = new DSSPEMResourceKeyPairParser();

    public DSSPEMResourceKeyPairParser() {
        super(KeyUtils.DSS_ALGORITHM, DSS_OID, BEGINNERS, ENDERS);
    }

    /**
     * <p>The ASN.1 syntax for the private key:</P>
     * <pre><code>
     * DSAPrivateKey ::= SEQUENCE {
     *      version Version,
     *      p       INTEGER,
     *      q       INTEGER,
     *      g       INTEGER,
     *      y       INTEGER,
     *      x       INTEGER
     * }
     * </code></pre>
     *
     * @param kf        The {@link KeyFactory} To use to generate the keys
     * @param s         The {@link InputStream} containing the encoded bytes
     * @param okToClose <code>true</code> if the method may close the input
     *                  stream regardless of success or failure
     * @return The recovered {@link KeyPair}
     * @throws IOException              If failed to read or decode the bytes
     * @throws GeneralSecurityException If failed to generate the keys
     */
    public static KeyPair decodeDSSKeyPair(KeyFactory kf, InputStream s, boolean okToClose)
            throws IOException, GeneralSecurityException {
        ASN1Object sequence;
        try (DERParser parser = new DERParser(NoCloseInputStream.resolveInputStream(s, okToClose))) {
            sequence = parser.readObject();
        }

        if (!ASN1Type.SEQUENCE.equals(sequence.getObjType())) {
            throw new IOException("Invalid DER: not a sequence: " + sequence.getObjType());
        }

        // Parse inside the sequence
        try (DERParser parser = sequence.createParser()) {
            // Skip version
            ASN1Object version = parser.readObject();
            if (version == null) {
                throw new StreamCorruptedException("No version");
            }

            BigInteger p = parser.readObject().asInteger();
            BigInteger q = parser.readObject().asInteger();
            BigInteger g = parser.readObject().asInteger();
            BigInteger y = parser.readObject().asInteger();
            BigInteger x = parser.readObject().asInteger();
            PublicKey pubKey = kf.generatePublic(new DSAPublicKeySpec(y, p, q, g));
            PrivateKey prvKey = kf.generatePrivate(new DSAPrivateKeySpec(x, p, q, g));
            return new KeyPair(pubKey, prvKey);
        }
    }

    @Override
    public Collection<KeyPair> extractKeyPairs(
            String resourceKey, String beginMarker, String endMarker, FilePasswordProvider passwordProvider, InputStream stream)
            throws IOException, GeneralSecurityException {
        KeyPair kp = decodeDSSKeyPair(SecurityUtils.getKeyFactory(KeyUtils.DSS_ALGORITHM), stream, false);
        return Collections.singletonList(kp);
    }
}

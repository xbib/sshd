package org.xbib.io.sshd.common.util.buffer.keys;

import org.xbib.io.sshd.common.cipher.ECCurves;
import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;

/**
 *
 */
public class ECBufferPublicKeyParser extends AbstractBufferPublicKeyParser<ECPublicKey> {
    public static final ECBufferPublicKeyParser INSTANCE = new ECBufferPublicKeyParser();

    public ECBufferPublicKeyParser() {
        super(ECPublicKey.class, ECCurves.KEY_TYPES);
    }

    @Override
    public ECPublicKey getRawPublicKey(String keyType, Buffer buffer) throws GeneralSecurityException {
        ValidateUtils.checkTrue(isKeyTypeSupported(keyType), "Unsupported key type: %s", keyType);
        ECCurves curve = ECCurves.fromKeyType(keyType);
        if (curve == null) {
            throw new NoSuchAlgorithmException("Unsupported raw public algorithm: " + keyType);
        }

        String curveName = curve.getName();
        ECParameterSpec params = curve.getParameters();
        return getRawECKey(curveName, params, buffer);
    }

    protected ECPublicKey getRawECKey(String expectedCurve, ECParameterSpec spec, Buffer buffer) throws GeneralSecurityException {
        String curveName = buffer.getString();
        if (!expectedCurve.equals(curveName)) {
            throw new InvalidKeySpecException("getRawECKey(" + expectedCurve + ") curve name does not match expected: " + curveName);
        }

        if (spec == null) {
            throw new InvalidKeySpecException("getRawECKey(" + expectedCurve + ") missing curve parameters");
        }

        byte[] octets = buffer.getBytes();
        ECPoint w;
        try {
            w = ECCurves.octetStringToEcPoint(octets);
        } catch (RuntimeException e) {
            throw new InvalidKeySpecException("getRawECKey(" + expectedCurve + ")"
                    + " cannot (" + e.getClass().getSimpleName() + ")"
                    + " retrieve W value: " + e.getMessage(),
                    e);
        }

        return generatePublicKey(KeyUtils.EC_ALGORITHM, new ECPublicKeySpec(w, spec));
    }
}

package org.xbib.io.sshd.common.kex;

import org.xbib.io.sshd.common.cipher.ECCurves;
import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.digest.Digest;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.security.SecurityUtils;

import javax.crypto.KeyAgreement;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Objects;

/**
 * Elliptic Curve Diffie-Hellman key agreement.
 */
public class ECDH extends AbstractDH {

    private ECParameterSpec params;
    private ECPoint e;
    private byte[] e_array;
    private ECPoint f;
    private KeyPairGenerator myKpairGen;
    private KeyAgreement myKeyAgree;

    public ECDH() throws Exception {
        this((ECParameterSpec) null);
    }

    public ECDH(String curveName) throws Exception {
        this(ValidateUtils.checkNotNull(ECCurves.fromCurveName(curveName), "Unknown curve name: %s", curveName));
    }

    public ECDH(ECCurves curve) throws Exception {
        this(Objects.requireNonNull(curve, "No known curve instance provided").getParameters());
    }

    public ECDH(ECParameterSpec paramSpec) throws Exception {
        myKpairGen = SecurityUtils.getKeyPairGenerator(KeyUtils.EC_ALGORITHM);
        myKeyAgree = SecurityUtils.getKeyAgreement("ECDH");
        params = paramSpec;
    }

    @Override
    public byte[] getE() throws Exception {
        if (e == null) {
            Objects.requireNonNull(params, "No ECParameterSpec(s)");
            myKpairGen.initialize(params);
            KeyPair myKpair = myKpairGen.generateKeyPair();
            myKeyAgree.init(myKpair.getPrivate());
            e = ((ECPublicKey) myKpair.getPublic()).getW();
            e_array = ECCurves.encodeECPoint(e, params);
        }
        return e_array;
    }

    @Override
    protected byte[] calculateK() throws Exception {
        Objects.requireNonNull(params, "No ECParameterSpec(s)");
        KeyFactory myKeyFac = SecurityUtils.getKeyFactory(KeyUtils.EC_ALGORITHM);
        ECPublicKeySpec keySpec = new ECPublicKeySpec(f, params);
        PublicKey yourPubKey = myKeyFac.generatePublic(keySpec);
        myKeyAgree.doPhase(yourPubKey, true);
        return stripLeadingZeroes(myKeyAgree.generateSecret());
    }

    public void setCurveParameters(ECParameterSpec paramSpec) {
        params = paramSpec;
    }

    @Override
    public void setF(byte[] f) {
        Objects.requireNonNull(params, "No ECParameterSpec(s)");
        this.f = ECCurves.octetStringToEcPoint(f);
    }

    @Override
    public Digest getHash() throws Exception {
        Objects.requireNonNull(params, "No ECParameterSpec(s)");
        ECCurves curve = Objects.requireNonNull(ECCurves.fromCurveParameters(params), "Unknown curve parameters");
        return curve.getDigestForParams();
    }
}

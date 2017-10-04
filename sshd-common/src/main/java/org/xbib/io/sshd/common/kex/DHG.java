package org.xbib.io.sshd.common.kex;

import org.xbib.io.sshd.common.Factory;
import org.xbib.io.sshd.common.digest.Digest;
import org.xbib.io.sshd.common.util.security.SecurityUtils;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;

/**
 * Diffie-Hellman key generator.
 */
public class DHG extends AbstractDH {

    private BigInteger p;
    private BigInteger g;
    private BigInteger e;  // my public key
    private byte[] e_array;
    private BigInteger f;  // your public key
    private KeyPairGenerator myKpairGen;
    private KeyAgreement myKeyAgree;
    private Factory<? extends Digest> factory;

    public DHG(Factory<? extends Digest> digestFactory) throws Exception {
        this(digestFactory, null, null);
    }

    public DHG(Factory<? extends Digest> digestFactory, BigInteger pValue, BigInteger gValue) throws Exception {
        myKpairGen = SecurityUtils.getKeyPairGenerator("DH");
        myKeyAgree = SecurityUtils.getKeyAgreement("DH");
        factory = digestFactory;
        p = pValue;
        g = gValue;
    }

    @Override
    public byte[] getE() throws Exception {
        if (e == null) {
            DHParameterSpec dhSkipParamSpec = new DHParameterSpec(p, g);
            myKpairGen.initialize(dhSkipParamSpec);
            KeyPair myKpair = myKpairGen.generateKeyPair();
            myKeyAgree.init(myKpair.getPrivate());
            e = ((javax.crypto.interfaces.DHPublicKey) (myKpair.getPublic())).getY();
            e_array = e.toByteArray();
        }
        return e_array;
    }

    @Override
    protected byte[] calculateK() throws Exception {
        KeyFactory myKeyFac = SecurityUtils.getKeyFactory("DH");
        DHPublicKeySpec keySpec = new DHPublicKeySpec(f, p, g);
        PublicKey yourPubKey = myKeyFac.generatePublic(keySpec);
        myKeyAgree.doPhase(yourPubKey, true);
        return stripLeadingZeroes(myKeyAgree.generateSecret());
    }

    public void setP(byte[] p) {
        setP(new BigInteger(p));
    }

    public void setG(byte[] g) {
        setG(new BigInteger(g));
    }

    @Override
    public void setF(byte[] f) {
        setF(new BigInteger(f));
    }

    public BigInteger getP() {
        return p;
    }

    public void setP(BigInteger p) {
        this.p = p;
    }

    public BigInteger getG() {
        return g;
    }

    public void setG(BigInteger g) {
        this.g = g;
    }

    public void setF(BigInteger f) {
        this.f = f;
    }

    @Override
    public Digest getHash() throws Exception {
        return factory.create();
    }
}

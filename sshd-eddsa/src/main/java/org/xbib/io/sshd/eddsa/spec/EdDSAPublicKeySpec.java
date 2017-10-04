package org.xbib.io.sshd.eddsa.spec;

import org.xbib.io.sshd.eddsa.math.GroupElement;

import java.security.spec.KeySpec;

/**
 *
 */
public class EdDSAPublicKeySpec implements KeySpec {
    private final GroupElement A;
    private final GroupElement Aneg;
    private final EdDSAParameterSpec spec;

    /**
     * @param pk   the public key
     * @param spec the parameter specification for this key
     * @throws IllegalArgumentException if key length is wrong
     */
    public EdDSAPublicKeySpec(byte[] pk, EdDSAParameterSpec spec) {
        if (pk.length != spec.getCurve().getField().getb() / 8)
            throw new IllegalArgumentException("public-key length is wrong");

        this.A = new GroupElement(spec.getCurve(), pk);
        // Precompute -A for use in verification.
        this.Aneg = A.negate();
        Aneg.precompute(false);
        this.spec = spec;
    }

    public EdDSAPublicKeySpec(GroupElement A, EdDSAParameterSpec spec) {
        this.A = A;
        this.Aneg = A.negate();
        Aneg.precompute(false);
        this.spec = spec;
    }

    public GroupElement getA() {
        return A;
    }

    public GroupElement getNegativeA() {
        return Aneg;
    }

    public EdDSAParameterSpec getParams() {
        return spec;
    }
}

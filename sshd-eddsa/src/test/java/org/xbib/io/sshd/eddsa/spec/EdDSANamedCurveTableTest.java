package org.xbib.io.sshd.eddsa.spec;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 *
 */
public class EdDSANamedCurveTableTest {
    /**
     * Ensure curve names are case-inspecific
     */
    @Test
    public void curveNamesAreCaseInspecific() {
        EdDSANamedCurveSpec mixed = EdDSANamedCurveTable.getByName("Ed25519");
        EdDSANamedCurveSpec lower = EdDSANamedCurveTable.getByName("ed25519");
        EdDSANamedCurveSpec upper = EdDSANamedCurveTable.getByName("ED25519");

        assertThat(lower, is(equalTo(mixed)));
        assertThat(upper, is(equalTo(mixed)));
    }
}

package org.xbib.io.sshd.eddsa.spec;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import org.xbib.io.sshd.eddsa.Utils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 *
 */
public class EdDSAPrivateKeySpecTest {
    static final byte[] ZERO_SEED = Utils.hexToBytes("0000000000000000000000000000000000000000000000000000000000000000");
    static final byte[] ZERO_H = Utils.hexToBytes("5046adc1dba838867b2bbbfdd0c3423e58b57970b5267a90f57960924a87f1960a6a85eaa642dac835424b5d7c8d637c00408c7a73da672b7f498521420b6dd3");
    static final byte[] ZERO_PK = Utils.hexToBytes("3b6a27bcceb6a42d62a3a8d02a6f0d73653215771de243a63ac048a18b59da29");

    static final EdDSANamedCurveSpec ed25519 = EdDSANamedCurveTable.getByName("Ed25519");

    @Rule
    public ExpectedException exception = ExpectedException.none();

    /**
     * Test method for {@link EdDSAPrivateKeySpec#EdDSAPrivateKeySpec(byte[], EdDSAParameterSpec)}.
     */
    @Test
    public void testEdDSAPrivateKeySpecFromSeed() {
        EdDSAPrivateKeySpec key = new EdDSAPrivateKeySpec(ZERO_SEED, ed25519);
        assertThat(key.getSeed(), is(equalTo(ZERO_SEED)));
        assertThat(key.getH(), is(equalTo(ZERO_H)));
        assertThat(key.getA().toByteArray(), is(equalTo(ZERO_PK)));
    }

    @Test
    public void incorrectSeedLengthThrows() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("seed length is wrong");
        EdDSAPrivateKeySpec key = new EdDSAPrivateKeySpec(new byte[2], ed25519);
    }

    /**
     * Test method for {@link EdDSAPrivateKeySpec#EdDSAPrivateKeySpec(EdDSAParameterSpec, byte[])}.
     */
    @Test
    public void testEdDSAPrivateKeySpecFromH() {
        EdDSAPrivateKeySpec key = new EdDSAPrivateKeySpec(ed25519, ZERO_H);
        assertThat(key.getSeed(), is(nullValue()));
        assertThat(key.getH(), is(equalTo(ZERO_H)));
        assertThat(key.getA().toByteArray(), is(equalTo(ZERO_PK)));
    }

    @Test
    public void incorrectHashLengthThrows() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("hash length is wrong");
        EdDSAPrivateKeySpec key = new EdDSAPrivateKeySpec(ed25519, new byte[2]);
    }
}

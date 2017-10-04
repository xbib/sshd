package org.xbib.io.sshd.eddsa.math.bigint;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.Random;

import org.xbib.io.sshd.eddsa.Utils;
import org.xbib.io.sshd.eddsa.math.Field;
import org.xbib.io.sshd.eddsa.math.FieldElement;
import org.xbib.io.sshd.eddsa.math.MathUtils;
import org.xbib.io.sshd.eddsa.math.AbstractFieldElementTest;
import org.junit.Test;

/**
 *
 */
public class BigIntegerFieldElementTest extends AbstractFieldElementTest {
    static final byte[] BYTES_ZERO = Utils.hexToBytes("0000000000000000000000000000000000000000000000000000000000000000");
    static final byte[] BYTES_ONE = Utils.hexToBytes("0100000000000000000000000000000000000000000000000000000000000000");
    static final byte[] BYTES_TEN = Utils.hexToBytes("0a00000000000000000000000000000000000000000000000000000000000000");

    static final Field ed25519Field = new Field(
            256, // b
            Utils.hexToBytes("edffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff7f"), // q
            new BigIntegerLittleEndianEncoding());

    static final FieldElement ZERO = new BigIntegerFieldElement(ed25519Field, BigInteger.ZERO);
    static final FieldElement ONE = new BigIntegerFieldElement(ed25519Field, BigInteger.ONE);
    static final FieldElement TWO = new BigIntegerFieldElement(ed25519Field, BigInteger.valueOf(2));

    protected FieldElement getRandomFieldElement() {
        BigInteger r;
        Random rnd = new Random();
        do {
            r = new BigInteger(255, rnd);
        } while (r.compareTo(getQ()) >= 0);
        return new BigIntegerFieldElement(ed25519Field, r);
    }

    protected BigInteger toBigInteger(FieldElement f) {
        return ((BigIntegerFieldElement)f).bi;
    }

    protected BigInteger getQ() {
        return MathUtils.getQ();
    }

    protected Field getField() {
        return ed25519Field;
    }

    /**
     * Test method for {@link BigIntegerFieldElement#BigIntegerFieldElement(Field, BigInteger)}.
     */
    @Test
    public void testFieldElementBigInteger() {
        assertThat(new BigIntegerFieldElement(ed25519Field, BigInteger.ZERO).bi, is(BigInteger.ZERO));
        assertThat(new BigIntegerFieldElement(ed25519Field, BigInteger.ONE).bi, is(BigInteger.ONE));
        assertThat(new BigIntegerFieldElement(ed25519Field, BigInteger.valueOf(2)).bi, is(BigInteger.valueOf(2)));
    }

    /**
     * Test method for {@link FieldElement#toByteArray()}.
     */
    @Test
    public void testToByteArray() {
        byte[] zero = ZERO.toByteArray();
        assertThat(zero.length, is(equalTo(BYTES_ZERO.length)));
        assertThat(zero, is(equalTo(BYTES_ZERO)));

        byte[] one = ONE.toByteArray();
        assertThat(one.length, is(equalTo(BYTES_ONE.length)));
        assertThat(one, is(equalTo(BYTES_ONE)));

        byte[] ten = new BigIntegerFieldElement(ed25519Field, BigInteger.TEN).toByteArray();
        assertThat(ten.length, is(equalTo(BYTES_TEN.length)));
        assertThat(ten, is(equalTo(BYTES_TEN)));
    }

    // region isNonZero

    protected FieldElement getZeroFieldElement() {
        return ZERO;
    }

    protected FieldElement getNonZeroFieldElement() {
        return TWO;
    }

    // endregion

    /**
     * Test method for {@link FieldElement#equals(Object)}.
     */
    @Test
    public void testEqualsObject() {
        assertThat(new BigIntegerFieldElement(ed25519Field, BigInteger.ZERO), is(equalTo(ZERO)));
        assertThat(new BigIntegerFieldElement(ed25519Field, BigInteger.valueOf(1000)), is(equalTo(new BigIntegerFieldElement(ed25519Field, BigInteger.valueOf(1000)))));
        assertThat(ONE, is(not(equalTo(TWO))));
    }

}

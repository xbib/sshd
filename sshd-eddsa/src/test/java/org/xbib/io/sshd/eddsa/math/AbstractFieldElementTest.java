package org.xbib.io.sshd.eddsa.math;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

/**
 * Tests rely on the BigInteger class.
 */
public abstract class AbstractFieldElementTest {

    protected abstract FieldElement getRandomFieldElement();
    protected abstract BigInteger toBigInteger(FieldElement f);
    protected abstract BigInteger getQ();
    protected abstract Field getField();

    protected abstract FieldElement getZeroFieldElement();
    protected abstract FieldElement getNonZeroFieldElement();

    @Test
    public void isNonZeroReturnsFalseIfFieldElementIsZero() {
        final FieldElement f = getZeroFieldElement();
        Assert.assertThat(f.isNonZero(), IsEqual.equalTo(false));
    }

    @Test
    public void isNonZeroReturnsTrueIfFieldElementIsNonZero() {
        final FieldElement f = getNonZeroFieldElement();
        Assert.assertThat(f.isNonZero(), IsEqual.equalTo(true));
    }

    @Test
    public void addReturnsCorrectResult() {
        for (int i=0; i<1000; i++) {
            final FieldElement f1 = getRandomFieldElement();
            final FieldElement f2 = getRandomFieldElement();
            final BigInteger b1 = toBigInteger(f1);
            final BigInteger b2 = toBigInteger(f2);

            final FieldElement f3 = f1.add(f2);
            final BigInteger b3 = toBigInteger(f3).mod(getQ());
            Assert.assertThat(b3, IsEqual.equalTo(b1.add(b2).mod(getQ())));
        }
    }

    @Test
    public void subtractReturnsCorrectResult() {
        for (int i=0; i<1000; i++) {
            final FieldElement f1 = getRandomFieldElement();
            final FieldElement f2 = getRandomFieldElement();
            final BigInteger b1 = toBigInteger(f1);
            final BigInteger b2 = toBigInteger(f2);

            final FieldElement f3 = f1.subtract(f2);
            final BigInteger b3 = toBigInteger(f3).mod(getQ());

            Assert.assertThat(b3, IsEqual.equalTo(b1.subtract(b2).mod(getQ())));
        }
    }

    @Test
    public void negateReturnsCorrectResult() {
        for (int i=0; i<1000; i++) {
            final FieldElement f1 = getRandomFieldElement();
            final BigInteger b1 = toBigInteger(f1);

            final FieldElement f2 = f1.negate();
            final BigInteger b2 = toBigInteger(f2).mod(getQ());

            Assert.assertThat(b2, IsEqual.equalTo(b1.negate().mod(getQ())));
        }
    }

    @Test
    public void multiplyReturnsCorrectResult() {
        for (int i=0; i<1000; i++) {
            final FieldElement f1 = getRandomFieldElement();
            final FieldElement f2 = getRandomFieldElement();
            final BigInteger b1 = toBigInteger(f1);
            final BigInteger b2 = toBigInteger(f2);

            final FieldElement f3 = f1.multiply(f2);
            final BigInteger b3 = toBigInteger(f3).mod(getQ());

            Assert.assertThat(b3, IsEqual.equalTo(b1.multiply(b2).mod(getQ())));
        }
    }

    @Test
    public void squareReturnsCorrectResult() {
        for (int i=0; i<1000; i++) {
            final FieldElement f1 = getRandomFieldElement();
            final BigInteger b1 = toBigInteger(f1);

            final FieldElement f2 = f1.square();
            final BigInteger b2 = toBigInteger(f2).mod(getQ());

            Assert.assertThat(b2, IsEqual.equalTo(b1.multiply(b1).mod(getQ())));
        }
    }

    @Test
    public void squareAndDoubleReturnsCorrectResult() {
        for (int i=0; i<1000; i++) {
            final FieldElement f1 = getRandomFieldElement();
            final BigInteger b1 = toBigInteger(f1);

            final FieldElement f2 = f1.squareAndDouble();
            final BigInteger b2 = toBigInteger(f2).mod(getQ());

            Assert.assertThat(b2, IsEqual.equalTo(b1.multiply(b1).multiply(new BigInteger("2")).mod(getQ())));
        }
    }

    @Test
    public void invertReturnsCorrectResult() {
        for (int i=0; i<1000; i++) {
            final FieldElement f1 = getRandomFieldElement();
            final BigInteger b1 = toBigInteger(f1);

            final FieldElement f2 = f1.invert();
            final BigInteger b2 = toBigInteger(f2).mod(getQ());

            Assert.assertThat(b2, IsEqual.equalTo(b1.modInverse(getQ())));
        }
    }

    @Test
    public void pow22523ReturnsCorrectResult() {
        for (int i=0; i<1000; i++) {
            final FieldElement f1 = getRandomFieldElement();
            final BigInteger b1 = toBigInteger(f1);

            final FieldElement f2 = f1.pow22523();
            final BigInteger b2 = toBigInteger(f2).mod(getQ());

            Assert.assertThat(b2, IsEqual.equalTo(b1.modPow(BigInteger.ONE.shiftLeft(252).subtract(new BigInteger("3")), getQ())));
        }
    }

    @Test
    public void cmovReturnsCorrectResult() {
        final FieldElement zero = getZeroFieldElement();
        final FieldElement nz = getNonZeroFieldElement();
        final FieldElement f = getRandomFieldElement();

        Assert.assertThat(zero.cmov(nz, 0), IsEqual.equalTo(zero));
        Assert.assertThat(zero.cmov(nz, 1), IsEqual.equalTo(nz));

        Assert.assertThat(f.cmov(nz, 0), IsEqual.equalTo(f));
        Assert.assertThat(f.cmov(nz, 1), IsEqual.equalTo(nz));
    }

    @Test
    public void equalsOnlyReturnsTrueForEquivalentObjects() {
        final FieldElement f1 = getRandomFieldElement();
        final FieldElement f2 = getField().getEncoding().decode(f1.toByteArray());
        final FieldElement f3 = getRandomFieldElement();
        final FieldElement f4 = getRandomFieldElement();

        Assert.assertThat(f1, IsEqual.equalTo(f2));
        Assert.assertThat(f1, IsNot.not(IsEqual.equalTo(f3)));
        Assert.assertThat(f1, IsNot.not(IsEqual.equalTo(f4)));
        Assert.assertThat(f3, IsNot.not(IsEqual.equalTo(f4)));
    }

    @Test
    public void hashCodesAreEqualForEquivalentObjects() {
        final FieldElement f1 = getRandomFieldElement();
        final FieldElement f2 = getField().getEncoding().decode(f1.toByteArray());
        final FieldElement f3 = getRandomFieldElement();
        final FieldElement f4 = getRandomFieldElement();

        Assert.assertThat(f1.hashCode(), IsEqual.equalTo(f2.hashCode()));
        Assert.assertThat(f1.hashCode(), IsNot.not(IsEqual.equalTo(f3.hashCode())));
        Assert.assertThat(f1.hashCode(), IsNot.not(IsEqual.equalTo(f4.hashCode())));
        Assert.assertThat(f3.hashCode(), IsNot.not(IsEqual.equalTo(f4.hashCode())));
    }

}

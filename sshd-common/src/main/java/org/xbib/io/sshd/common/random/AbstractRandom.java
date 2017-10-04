package org.xbib.io.sshd.common.random;

/**
 *
 */
public abstract class AbstractRandom implements Random {
    protected AbstractRandom() {
        super();
    }

    @Override
    public String toString() {
        return getName();
    }
}

package org.xbib.io.sshd.common.util.security.bouncycastle;

import org.xbib.io.sshd.common.random.AbstractRandomFactory;
import org.xbib.io.sshd.common.random.Random;
import org.xbib.io.sshd.common.util.security.SecurityUtils;

/**
 * Named factory for the BouncyCastle <code>Random</code>.
 */
public final class BouncyCastleRandomFactory extends AbstractRandomFactory {
    public static final String NAME = "bouncycastle";
    public static final BouncyCastleRandomFactory INSTANCE = new BouncyCastleRandomFactory();

    public BouncyCastleRandomFactory() {
        super(NAME);
    }

    @Override
    public boolean isSupported() {
        return SecurityUtils.isBouncyCastleRegistered();
    }

    @Override
    public Random create() {
        return new BouncyCastleRandom();
    }
}

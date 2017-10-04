package org.xbib.io.sshd.common.util.security.bouncycastle;

import org.bouncycastle.crypto.prng.RandomGenerator;
import org.bouncycastle.crypto.prng.VMPCRandomGenerator;
import org.xbib.io.sshd.common.random.AbstractRandom;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.security.SecurityUtils;

import java.security.SecureRandom;

/**
 * BouncyCastle <code>Random</code>.
 * This pseudo random number generator uses the a very fast PRNG from BouncyCastle.
 * The JRE random will be used when creating a new generator to add some random
 * data to the seed.
 */
public final class BouncyCastleRandom extends AbstractRandom {
    public static final String NAME = SecurityUtils.BOUNCY_CASTLE;
    private final RandomGenerator random;

    public BouncyCastleRandom() {
        ValidateUtils.checkTrue(SecurityUtils.isBouncyCastleRegistered(), "BouncyCastle not registered");
        this.random = new VMPCRandomGenerator();
        byte[] seed = new SecureRandom().generateSeed(8);
        this.random.addSeedMaterial(seed);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void fill(byte[] bytes, int start, int len) {
        this.random.nextBytes(bytes, start, len);
    }

    /**
     * Returns a pseudo-random uniformly distributed {@code int}
     * in the half-open range [0, n).
     */
    @Override
    public int random(int n) {
        ValidateUtils.checkTrue(n > 0, "Limit must be positive: %d", n);
        if ((n & -n) == n) {
            return (int) ((n * (long) next(31)) >> 31);
        }

        int bits;
        int val;
        do {
            bits = next(31);
            val = bits % n;
        } while (bits - val + (n - 1) < 0);
        return val;
    }

    private int next(int numBits) {
        int bytes = (numBits + 7) / 8;
        byte next[] = new byte[bytes];
        int ret = 0;
        random.nextBytes(next);
        for (int i = 0; i < bytes; i++) {
            ret = (next[i] & 0xFF) | (ret << 8);
        }
        return ret >>> (bytes * 8 - numBits);
    }
}
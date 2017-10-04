package org.xbib.io.sshd.common.random;

import java.security.SecureRandom;

/**
 * A <code>Random</code> implementation using the built-in {@link SecureRandom} PRNG.
 */
public class JceRandom extends AbstractRandom {
    public static final String NAME = "JCE";
    private final SecureRandom random = new SecureRandom();
    private byte[] tmp = new byte[16];

    public JceRandom() {
        super();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public synchronized void fill(byte[] foo, int start, int len) {
        if ((start == 0) && (len == foo.length)) {
            random.nextBytes(foo);
        } else {
            if (len > tmp.length) {
                tmp = new byte[len];
            }
            random.nextBytes(tmp);
            System.arraycopy(tmp, 0, foo, start, len);
        }
    }

    @Override
    public synchronized int random(int n) {
        return random.nextInt(n);
    }
}

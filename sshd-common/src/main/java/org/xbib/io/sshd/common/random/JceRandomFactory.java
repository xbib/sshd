package org.xbib.io.sshd.common.random;

/**
 * Named factory for the JCE <code>Random</code>.
 */
public class JceRandomFactory extends AbstractRandomFactory {
    public static final String NAME = "default";
    public static final JceRandomFactory INSTANCE = new JceRandomFactory();

    public JceRandomFactory() {
        super(NAME);
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public Random create() {
        return new JceRandom();
    }
}
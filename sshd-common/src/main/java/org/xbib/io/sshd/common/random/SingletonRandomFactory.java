package org.xbib.io.sshd.common.random;

import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.OptionalFeature;

import java.util.Objects;

/**
 * A random factory wrapper that uses a single random instance.
 * The underlying random instance has to be thread safe.
 */
public class SingletonRandomFactory extends AbstractRandom implements RandomFactory {

    private final NamedFactory<Random> factory;
    private final Random random;

    public SingletonRandomFactory(NamedFactory<Random> factory) {
        this.factory = Objects.requireNonNull(factory, "No factory");
        this.random = Objects.requireNonNull(factory.create(), "No random instance created");
    }

    @Override
    public boolean isSupported() {
        if (factory instanceof OptionalFeature) {
            return ((OptionalFeature) factory).isSupported();
        } else {
            return true;
        }
    }

    @Override
    public void fill(byte[] bytes, int start, int len) {
        random.fill(bytes, start, len);
    }

    @Override
    public int random(int max) {
        return random.random(max);
    }

    @Override
    public String getName() {
        return factory.getName();
    }

    @Override
    public Random create() {
        return this;
    }
}

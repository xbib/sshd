package org.xbib.io.sshd.common.util;

import java.util.function.Supplier;

/**
 * A generic builder interface
 *
 * @param <T> Type of object being built
 */
@FunctionalInterface
public interface ObjectBuilder<T> extends Supplier<T> {
    @Override
    default T get() {
        return build();
    }

    T build();
}

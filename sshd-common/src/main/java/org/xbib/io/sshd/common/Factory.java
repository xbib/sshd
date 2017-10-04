package org.xbib.io.sshd.common;

import java.util.function.Supplier;

/**
 * Factory is a simple interface that is used to create other objects.
 *
 * @param <T> type of objets this factory will create
 */
@FunctionalInterface
public interface Factory<T> extends Supplier<T> {

    @Override
    default T get() {
        return create();
    }

    /**
     * @return A new instance
     */
    T create();
}

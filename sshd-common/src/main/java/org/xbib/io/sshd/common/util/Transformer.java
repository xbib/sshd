package org.xbib.io.sshd.common.util;

import java.util.Objects;
import java.util.function.Function;

/**
 * @param <I> Input type
 * @param <O> Output type
 */
@FunctionalInterface
public interface Transformer<I, O> extends Function<I, O> {
    /**
     * Invokes {@link Objects#toString(Object, String)} on the argument
     * with {@code null} as the value to return if argument is {@code null}
     */
    Function<Object, String> TOSTRING = input -> Objects.toString(input, null);

    /**
     * Returns {@link Enum#name()} or {@code null} if argument is {@code null}
     */
    Function<Enum<?>, String> ENUM_NAME_EXTRACTOR = input -> (input == null) ? null : input.name();

    static <U extends V, V> Transformer<U, V> identity() {
        return input -> input;
    }

    @Override
    default O apply(I input) {
        return transform(input);
    }

    /**
     * @param input Input value
     * @return Transformed output value
     */
    O transform(I input);
}

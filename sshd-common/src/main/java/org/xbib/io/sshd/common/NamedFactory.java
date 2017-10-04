package org.xbib.io.sshd.common;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A named factory is a factory identified by a name.
 * Such names are used mainly in the algorithm negotiation at the beginning of the SSH connection.
 *
 * @param <T> The create object instance type
 */
public interface NamedFactory<T> extends Factory<T>, NamedResource {
    /**
     * Create an instance of the specified name by looking up the needed factory
     * in the list.
     *
     * @param factories list of available factories
     * @param name      the factory name to use
     * @param <T>       type of object to create
     * @return a newly created object or {@code null} if the factory is not in the list
     */
    static <T> T create(Collection<? extends NamedFactory<T>> factories, String name) {
        NamedFactory<? extends T> f = NamedResource.findByName(name, String.CASE_INSENSITIVE_ORDER, factories);
        if (f != null) {
            return f.create();
        } else {
            return null;
        }
    }

    static <S extends OptionalFeature, T, E extends NamedFactory<T>> List<NamedFactory<T>> setUpTransformedFactories(
            boolean ignoreUnsupported, Collection<? extends S> preferred, Function<? super S, ? extends E> xform) {
        return preferred.stream()
                .filter(f -> ignoreUnsupported || f.isSupported())
                .map(xform)
                .collect(Collectors.toList());
    }

    static <T, E extends NamedFactory<T> & OptionalFeature> List<NamedFactory<T>> setUpBuiltinFactories(
            boolean ignoreUnsupported, Collection<? extends E> preferred) {
        return preferred.stream()
                .filter(f -> ignoreUnsupported || f.isSupported())
                .collect(Collectors.toList());
    }
}

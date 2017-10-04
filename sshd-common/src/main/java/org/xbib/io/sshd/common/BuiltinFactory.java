package org.xbib.io.sshd.common;

import org.xbib.io.sshd.common.util.GenericUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A named optional factory.
 *
 * @param <T> The create object instance type
 */
public interface BuiltinFactory<T> extends NamedFactory<T>, OptionalFeature {
    static <T, E extends BuiltinFactory<T>> List<NamedFactory<T>> setUpFactories(
            boolean ignoreUnsupported, Collection<? extends E> preferred) {
        return GenericUtils.stream(preferred)
                .filter(f -> ignoreUnsupported || f.isSupported())
                .collect(Collectors.toList());
    }
}

package org.xbib.io.sshd.common.keyprovider;

import org.xbib.io.sshd.common.util.GenericUtils;

import java.security.KeyPair;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 */
@FunctionalInterface
public interface KeyIdentityProvider {
    /**
     * An &quot;empty&quot; implementation of {@link KeyIdentityProvider} that
     * returns an empty group of key pairs
     */
    KeyIdentityProvider EMPTY_KEYS_PROVIDER = new KeyIdentityProvider() {
        @Override
        public Iterable<KeyPair> loadKeys() {
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return "EMPTY";
        }
    };

    /**
     * Invokes {@link KeyIdentityProvider#loadKeys()} and returns the result - ignores
     * {@code null} providers (i.e., returns an empty iterable instance)
     */
    Function<KeyIdentityProvider, Iterable<KeyPair>> LOADER = p ->
            (p == null) ? Collections.emptyList() : p.loadKeys();

    /**
     * Creates a &quot;unified&quot; {@link Iterator} of {@link KeyPair}s out of 2 possible
     * {@link KeyIdentityProvider}
     *
     * @param identities The registered keys identities
     * @param keys       Extra available key pairs
     * @return The wrapping iterator
     * @see #resolveKeyIdentityProvider(KeyIdentityProvider, KeyIdentityProvider)
     */
    static Iterator<KeyPair> iteratorOf(KeyIdentityProvider identities, KeyIdentityProvider keys) {
        return iteratorOf(resolveKeyIdentityProvider(identities, keys));
    }

    /**
     * Resolves a non-{@code null} iterator of the available keys
     *
     * @param provider The {@link KeyIdentityProvider} - ignored if {@code null}
     * @return A non-{@code null} iterator - which may be empty if no provider or no keys
     */
    static Iterator<KeyPair> iteratorOf(KeyIdentityProvider provider) {
        return GenericUtils.iteratorOf((provider == null) ? null : provider.loadKeys());
    }

    /**
     * <P>Creates a &quot;unified&quot; {@link KeyIdentityProvider} out of 2 possible ones
     * as follows:</P>
     * <UL>
     * <LI>If both are {@code null} then return {@code null}.</LI>
     * <LI>If either one is {@code null} then use the non-{@code null} one.</LI>
     * <LI>If both are the same instance then use it.</LI>
     * <LI>Otherwise, returns a wrapper that groups both providers.</LI>
     * </UL>
     *
     * @param identities The registered key pair identities
     * @param keys       The extra available key pairs
     * @return The resolved provider
     * @see #multiProvider(KeyIdentityProvider...)
     */
    static KeyIdentityProvider resolveKeyIdentityProvider(KeyIdentityProvider identities, KeyIdentityProvider keys) {
        if ((keys == null) || (identities == keys)) {
            return identities;
        } else if (identities == null) {
            return keys;
        } else {
            return multiProvider(identities, keys);
        }
    }

    /**
     * Wraps a group of {@link KeyIdentityProvider} into a single one
     *
     * @param providers The providers - ignored if {@code null}/empty (i.e., returns
     *                  {@link #EMPTY_KEYS_PROVIDER})
     * @return The wrapping provider
     * @see #multiProvider(Collection)
     */
    static KeyIdentityProvider multiProvider(KeyIdentityProvider... providers) {
        return multiProvider(GenericUtils.asList(providers));
    }

    /**
     * Wraps a group of {@link KeyIdentityProvider} into a single one
     *
     * @param providers The providers - ignored if {@code null}/empty (i.e., returns
     *                  {@link #EMPTY_KEYS_PROVIDER})
     * @return The wrapping provider
     */
    static KeyIdentityProvider multiProvider(Collection<? extends KeyIdentityProvider> providers) {
        return GenericUtils.isEmpty(providers) ? EMPTY_KEYS_PROVIDER : wrap(iterableOf(providers));
    }

    /**
     * Wraps a group of {@link KeyIdentityProvider} into an {@link Iterable} of {@link KeyPair}s
     *
     * @param providers The group of providers - ignored if {@code null}/empty (i.e., returns an
     *                  empty iterable instance)
     * @return The wrapping iterable
     */
    static Iterable<KeyPair> iterableOf(Collection<? extends KeyIdentityProvider> providers) {
        Iterable<Supplier<Iterable<KeyPair>>> keysSuppliers =
                GenericUtils.<KeyIdentityProvider, Supplier<Iterable<KeyPair>>>wrapIterable(providers, p -> p::loadKeys);
        return GenericUtils.multiIterableSuppliers(keysSuppliers);
    }

    /**
     * Wraps a group of {@link KeyPair}s into a {@link KeyIdentityProvider}
     *
     * @param pairs The key pairs - ignored if {@code null}/empty (i.e., returns
     *              {@link #EMPTY_KEYS_PROVIDER}).
     * @return The provider wrapper
     */
    static KeyIdentityProvider wrap(KeyPair... pairs) {
        return wrap(GenericUtils.asList(pairs));
    }

    /**
     * Wraps a group of {@link KeyPair}s into a {@link KeyIdentityProvider}
     *
     * @param pairs The key pairs {@link Iterable} - ignored if {@code null} (i.e., returns
     *              {@link #EMPTY_KEYS_PROVIDER}).
     * @return The provider wrapper
     */
    static KeyIdentityProvider wrap(final Iterable<KeyPair> pairs) {
        return (pairs == null) ? EMPTY_KEYS_PROVIDER : () -> pairs;
    }

    /**
     * Load available keys.
     *
     * @return an {@link Iterable} instance of available keys - ignored if {@code null}
     */
    Iterable<KeyPair> loadKeys();
}

package org.xbib.io.sshd.client.auth.password;

import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.common.util.GenericUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *
 */
@FunctionalInterface
public interface PasswordIdentityProvider {

    /**
     * An &quot;empty&quot; implementation of {@link PasswordIdentityProvider} that returns
     * and empty group of passwords
     */
    PasswordIdentityProvider EMPTY_PASSWORDS_PROVIDER = new PasswordIdentityProvider() {
        @Override
        public Iterable<String> loadPasswords() {
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return "EMPTY";
        }
    };

    /**
     * Invokes {@link PasswordIdentityProvider#loadPasswords()} and returns the result.
     * Ignores {@code null} providers (i.e., returns an empty iterable instance)
     */
    Function<PasswordIdentityProvider, Iterable<String>> LOADER = p ->
            (p == null) ? Collections.emptyList() : p.loadPasswords();

    /**
     * Creates a &quot;unified&quot; {@link Iterator} of passwords out of the registered
     * passwords and the extra available ones as a single iterator of passwords
     *
     * @param session The {@link ClientSession} - ignored if {@code null} (i.e., empty
     *                iterator returned)
     * @return The wrapping iterator
     * @see ClientSession#getRegisteredIdentities()
     * @see ClientSession#getPasswordIdentityProvider()
     */
    static Iterator<String> iteratorOf(ClientSession session) {
        return (session == null) ? Collections.<String>emptyIterator() : iteratorOf(session.getRegisteredIdentities(), session.getPasswordIdentityProvider());
    }

    /**
     * Creates a &quot;unified&quot; {@link Iterator} of passwords out of 2 possible
     * {@link PasswordIdentityProvider}
     *
     * @param identities The registered passwords
     * @param passwords  Extra available passwords
     * @return The wrapping iterator
     * @see #resolvePasswordIdentityProvider(PasswordIdentityProvider, PasswordIdentityProvider)
     */
    static Iterator<String> iteratorOf(PasswordIdentityProvider identities, PasswordIdentityProvider passwords) {
        return iteratorOf(resolvePasswordIdentityProvider(identities, passwords));
    }

    /**
     * Resolves a non-{@code null} iterator of the available passwords
     *
     * @param provider The {@link PasswordIdentityProvider} - ignored if {@code null} (i.e.,
     *                 return an empty iterator)
     * @return A non-{@code null} iterator - which may be empty if no provider or no passwords
     */
    static Iterator<String> iteratorOf(PasswordIdentityProvider provider) {
        return GenericUtils.iteratorOf((provider == null) ? null : provider.loadPasswords());
    }

    /**
     * <P>Creates a &quot;unified&quot; {@link PasswordIdentityProvider} out of 2 possible ones
     * as follows:</P>
     * <UL>
     * <LI>If both are {@code null} then return {@code null}.</LI>
     * <LI>If either one is {@code null} then use the non-{@code null} one.</LI>
     * <LI>If both are the same instance then use it.
     * <LI>Otherwise, returns a wrapper that groups both providers.</LI>
     * </UL>
     *
     * @param identities The registered passwords
     * @param passwords  The extra available passwords
     * @return The resolved provider
     * @see #multiProvider(PasswordIdentityProvider...)
     */
    static PasswordIdentityProvider resolvePasswordIdentityProvider(PasswordIdentityProvider identities, PasswordIdentityProvider passwords) {
        if ((passwords == null) || (identities == passwords)) {
            return identities;
        } else if (identities == null) {
            return passwords;
        } else {
            return multiProvider(identities, passwords);
        }
    }

    /**
     * Wraps a group of {@link PasswordIdentityProvider} into a single one
     *
     * @param providers The providers - ignored if {@code null}/empty (i.e., returns
     *                  {@link #EMPTY_PASSWORDS_PROVIDER}
     * @return The wrapping provider
     * @see #multiProvider(Collection)
     */
    static PasswordIdentityProvider multiProvider(PasswordIdentityProvider... providers) {
        return multiProvider(GenericUtils.asList(providers));
    }

    /**
     * Wraps a group of {@link PasswordIdentityProvider} into a single one
     *
     * @param providers The providers - ignored if {@code null}/empty (i.e., returns
     *                  {@link #EMPTY_PASSWORDS_PROVIDER}
     * @return The wrapping provider
     */
    static PasswordIdentityProvider multiProvider(Collection<? extends PasswordIdentityProvider> providers) {
        return GenericUtils.isEmpty(providers) ? EMPTY_PASSWORDS_PROVIDER : wrap(iterableOf(providers));
    }

    /**
     * Wraps a group of {@link PasswordIdentityProvider} into an {@link Iterable} of their combined passwords
     *
     * @param providers The providers - ignored if {@code null}/empty (i.e., returns an empty iterable instance)
     * @return The wrapping iterable
     */
    static Iterable<String> iterableOf(Collection<? extends PasswordIdentityProvider> providers) {
        Iterable<Supplier<Iterable<String>>> passwordSuppliers =
                GenericUtils.<PasswordIdentityProvider, Supplier<Iterable<String>>>wrapIterable(providers, p -> p::loadPasswords);
        return GenericUtils.multiIterableSuppliers(passwordSuppliers);
    }

    /**
     * Wraps a group of passwords into a {@link PasswordIdentityProvider}
     *
     * @param passwords The passwords - ignored if {@code null}/empty
     *                  (i.e., returns {@link #EMPTY_PASSWORDS_PROVIDER})
     * @return The provider wrapper
     */
    static PasswordIdentityProvider wrap(String... passwords) {
        return wrap(GenericUtils.asList(passwords));
    }

    /**
     * Wraps a group of passwords into a {@link PasswordIdentityProvider}
     *
     * @param passwords The passwords {@link Iterable} - ignored if {@code null}
     *                  (i.e., returns {@link #EMPTY_PASSWORDS_PROVIDER})
     * @return The provider wrapper
     */
    static PasswordIdentityProvider wrap(Iterable<String> passwords) {
        return (passwords == null) ? EMPTY_PASSWORDS_PROVIDER : () -> passwords;
    }

    /**
     * @return The currently available passwords - ignored if {@code null}
     */
    Iterable<String> loadPasswords();
}

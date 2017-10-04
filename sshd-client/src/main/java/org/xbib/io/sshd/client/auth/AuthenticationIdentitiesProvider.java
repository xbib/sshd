package org.xbib.io.sshd.client.auth;

import org.xbib.io.sshd.client.auth.password.PasswordIdentityProvider;
import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.keyprovider.KeyIdentityProvider;
import org.xbib.io.sshd.common.util.GenericUtils;

import java.security.KeyPair;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 */
public interface AuthenticationIdentitiesProvider extends KeyIdentityProvider, PasswordIdentityProvider {

    /**
     * Compares 2 password identities - returns zero ONLY if <U>both</U> compared
     * objects are {@link String}s and equal to each other
     */
    Comparator<Object> PASSWORD_IDENTITY_COMPARATOR = (o1, o2) -> {
        if (!(o1 instanceof String) || !(o2 instanceof String)) {
            return -1;
        } else {
            return ((String) o1).compareTo((String) o2);
        }
    };

    /**
     * Compares 2 {@link KeyPair} identities - returns zero ONLY if <U>both</U> compared
     * objects are {@link KeyPair}s and equal to each other
     */
    Comparator<Object> KEYPAIR_IDENTITY_COMPARATOR = (o1, o2) -> {
        if ((!(o1 instanceof KeyPair)) || (!(o2 instanceof KeyPair))) {
            return -1;
        } else if (KeyUtils.compareKeyPairs((KeyPair) o1, (KeyPair) o2)) {
            return 0;
        } else {
            return 1;
        }
    };

    static int findIdentityIndex(List<?> identities, Comparator<? super Object> comp, Object target) {
        for (int index = 0; index < identities.size(); index++) {
            Object value = identities.get(index);
            if (comp.compare(value, target) == 0) {
                return index;
            }
        }

        return -1;
    }

    /**
     * @param identities The {@link Iterable} identities - OK if {@code null}/empty
     * @return An {@link AuthenticationIdentitiesProvider} wrapping the identities
     */
    static AuthenticationIdentitiesProvider wrap(Iterable<?> identities) {
        return new AuthenticationIdentitiesProvider() {
            @Override
            public Iterable<KeyPair> loadKeys() {
                return selectIdentities(KeyPair.class);
            }

            @Override
            public Iterable<String> loadPasswords() {
                return selectIdentities(String.class);
            }

            @Override
            public Iterable<?> loadIdentities() {
                return selectIdentities(Object.class);
            }

            // NOTE: returns a NEW Collection on every call so that the original
            //      identities remain unchanged
            private <T> Collection<T> selectIdentities(Class<T> type) {
                Collection<T> matches = null;
                for (Iterator<?> iter = GenericUtils.iteratorOf(identities); iter.hasNext(); ) {
                    Object o = iter.next();
                    Class<?> t = o.getClass();
                    if (!type.isAssignableFrom(t)) {
                        continue;
                    }
                    if (matches == null) {
                        matches = new LinkedList<>();
                    }
                    matches.add(type.cast(o));
                }
                return (matches == null) ? Collections.<T>emptyList() : matches;
            }
        };
    }

    /**
     * @return All the currently available identities - passwords, keys, etc...
     */
    Iterable<?> loadIdentities();
}

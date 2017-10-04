package org.xbib.io.sshd.client.auth.hostbased;

import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.Pair;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 */
@FunctionalInterface
public interface HostKeyIdentityProvider {
    static Iterator<Pair<KeyPair, List<X509Certificate>>> iteratorOf(HostKeyIdentityProvider provider) {
        return GenericUtils.iteratorOf((provider == null) ? null : provider.loadHostKeys());
    }

    static HostKeyIdentityProvider wrap(KeyPair... pairs) {
        return wrap(GenericUtils.asList(pairs));
    }

    static HostKeyIdentityProvider wrap(Iterable<? extends KeyPair> pairs) {
        return () -> GenericUtils.wrapIterable(pairs, kp -> new Pair<>(kp, Collections.<X509Certificate>emptyList()));
    }

    /**
     * @return The host keys as a {@link Pair} of key + certificates (which can be {@code null}/empty)
     */
    Iterable<Pair<KeyPair, List<X509Certificate>>> loadHostKeys();
}

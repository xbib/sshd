package org.xbib.io.sshd.server.auth.pubkey;

import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;
import org.xbib.io.sshd.server.session.ServerSession;

import java.security.PublicKey;
import java.util.Collection;
import java.util.Collections;

/**
 * Checks against a {@link Collection} of {@link PublicKey}s.
 */
public class KeySetPublickeyAuthenticator extends AbstractLoggingBean implements PublickeyAuthenticator {
    private final Collection<? extends PublicKey> keySet;

    public KeySetPublickeyAuthenticator(Collection<? extends PublicKey> keySet) {
        this.keySet = (keySet == null) ? Collections.emptyList() : keySet;
    }

    public final Collection<? extends PublicKey> getKeySet() {
        return keySet;
    }

    @Override
    public boolean authenticate(String username, PublicKey key, ServerSession session) {
        return authenticate(username, key, session, getKeySet());
    }

    public boolean authenticate(String username, PublicKey key, ServerSession session, Collection<? extends PublicKey> keys) {
        if (GenericUtils.isEmpty(keys)) {
            return false;
        }

        PublicKey matchKey = KeyUtils.findMatchingKey(key, keys);
        boolean matchFound = matchKey != null;
        return matchFound;
    }
}
package org.xbib.io.sshd.server.auth.pubkey;

import org.xbib.io.sshd.common.RuntimeSshException;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.session.SessionListener;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;
import org.xbib.io.sshd.server.session.ServerSession;

import java.security.PublicKey;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches the result per session
 */
public class CachingPublicKeyAuthenticator extends AbstractLoggingBean implements PublickeyAuthenticator, SessionListener {

    protected final PublickeyAuthenticator authenticator;
    protected final Map<Session, Map<PublicKey, Boolean>> cache = new ConcurrentHashMap<>();

    public CachingPublicKeyAuthenticator(PublickeyAuthenticator authenticator) {
        this.authenticator = Objects.requireNonNull(authenticator, "No delegate authenticator");
    }

    @Override
    public boolean authenticate(String username, PublicKey key, ServerSession session) {
        Map<PublicKey, Boolean> map = cache.get(session);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            cache.put(session, map);
            session.addSessionListener(this);
        }

        Boolean result = map.get(key);
        if (result == null) {
            try {
                result = authenticator.authenticate(username, key, session);
            } catch (Error e) {
                throw new RuntimeSshException(e);
            }
            map.put(key, result);
        } else {
        }

        return result;
    }

    @Override
    public void sessionCreated(Session session) {
        // ignored
    }

    @Override
    public void sessionEvent(Session session, Event event) {
        // ignored
    }

    @Override
    public void sessionException(Session session, Throwable t) {
        sessionClosed(session);
    }

    @Override
    public void sessionClosed(Session session) {
        Map<PublicKey, Boolean> map = cache.remove(session);
        if (map == null) {
        } else {
        }
        session.removeSessionListener(this);
    }
}

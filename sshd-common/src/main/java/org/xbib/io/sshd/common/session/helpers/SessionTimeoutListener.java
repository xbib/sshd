package org.xbib.io.sshd.common.session.helpers;

import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.session.SessionListener;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Task that iterates over all currently open {@link AbstractSession}s and checks each of them for timeouts. If
 * the {@link AbstractSession} has timed out (either auth or idle timeout), the session will be disconnected.
 */
public class SessionTimeoutListener extends AbstractLoggingBean implements SessionListener, Runnable {
    private final Set<AbstractSession> sessions = new CopyOnWriteArraySet<>();

    public SessionTimeoutListener() {
        super();
    }

    @Override
    public void sessionCreated(Session session) {
        if ((session instanceof AbstractSession) && ((session.getAuthTimeout() > 0L) || (session.getIdleTimeout() > 0L))) {
            sessions.add((AbstractSession) session);
        } else {
        }
    }

    @Override
    public void sessionEvent(Session session, Event event) {
        // ignored
    }

    @Override
    public void sessionException(Session session, Throwable t) {
        sessionClosed(session);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public void sessionClosed(Session s) {
        if (sessions.remove(s)) {
        } else {
        }
    }

    @Override
    public void run() {
        for (AbstractSession session : sessions) {
            try {
                session.checkForTimeouts();
            } catch (Exception e) {
            }
        }
    }
}

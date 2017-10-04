package org.xbib.io.sshd.common.session;

/**
 * Marker interface for classes that allow to add/remove session listeners.
 * <B>Note:</B> if adding/removing listeners while connections are being
 * established and/or torn down there are no guarantees as to the order of
 * the calls to the recently added/removed listener's methods in the interim.
 * The correct order is guaranteed only as of the <U>next</U> session after
 * the listener has been added/removed.
 */
public interface SessionListenerManager {
    /**
     * Add a session listener.
     *
     * @param listener The {@link org.xbib.io.sshd.common.session.SessionListener} to add - not {@code null}
     */
    void addSessionListener(org.xbib.io.sshd.common.session.SessionListener listener);

    /**
     * Remove a session listener.
     *
     * @param listener The {@link org.xbib.io.sshd.common.session.SessionListener} to remove
     */
    void removeSessionListener(org.xbib.io.sshd.common.session.SessionListener listener);

    /**
     * @return A (never {@code null} proxy {@link org.xbib.io.sshd.common.session.SessionListener} that represents
     * all the currently registered listeners. Any method invocation on the proxy
     * is replicated to the currently registered listeners
     */
    SessionListener getSessionListenerProxy();
}

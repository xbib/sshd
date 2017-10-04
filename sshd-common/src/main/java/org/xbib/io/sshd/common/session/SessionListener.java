package org.xbib.io.sshd.common.session;

import org.xbib.io.sshd.common.kex.KexProposalOption;
import org.xbib.io.sshd.common.util.SshdEventListener;

import java.util.Map;

/**
 * Represents an interface receiving session events.
 */
public interface SessionListener extends SshdEventListener {
    static <L extends SessionListener> L validateListener(L listener) {
        return SshdEventListener.validateListener(listener, SessionListener.class.getSimpleName());
    }

    /**
     * A new session just been created
     *
     * @param session The created {@link org.xbib.io.sshd.common.session.Session}
     */
    default void sessionCreated(org.xbib.io.sshd.common.session.Session session) {
        // ignored
    }

    /**
     * Signals the start of the negotiation options handling
     *
     * @param session        The referenced {@link org.xbib.io.sshd.common.session.Session}
     * @param clientProposal The client proposal options (un-modifiable)
     * @param serverProposal The server proposal options (un-modifiable)
     */
    default void sessionNegotiationStart(org.xbib.io.sshd.common.session.Session session,
                                         Map<KexProposalOption, String> clientProposal, Map<KexProposalOption, String> serverProposal) {
        // ignored
    }

    /**
     * Signals the end of the negotiation options handling
     *
     * @param session           The referenced {@link org.xbib.io.sshd.common.session.Session}
     * @param clientProposal    The client proposal options (un-modifiable)
     * @param serverProposal    The server proposal options (un-modifiable)
     * @param negotiatedOptions The successfully negotiated options so far
     *                          - even if exception occurred (un-modifiable)
     * @param reason            Negotiation end reason - {@code null} if successful
     */
    default void sessionNegotiationEnd(org.xbib.io.sshd.common.session.Session session,
                                       Map<KexProposalOption, String> clientProposal, Map<KexProposalOption, String> serverProposal,
                                       Map<KexProposalOption, String> negotiatedOptions, Throwable reason) {
        // ignored
    }

    /**
     * An event has been triggered
     *
     * @param session The referenced {@link org.xbib.io.sshd.common.session.Session}
     * @param event   The generated {@link Event}
     */
    default void sessionEvent(org.xbib.io.sshd.common.session.Session session, Event event) {
        // ignored
    }

    /**
     * An exception was caught and the session will be closed
     * (if not already so). <B>Note:</B> the code makes no guarantee
     * that at this stage {@link #sessionClosed(org.xbib.io.sshd.common.session.Session)} will be called
     * or perhaps has already been called
     *
     * @param session The referenced {@link org.xbib.io.sshd.common.session.Session}
     * @param t       The caught exception
     */
    default void sessionException(org.xbib.io.sshd.common.session.Session session, Throwable t) {
        // ignored
    }

    /**
     * A session has been closed
     *
     * @param session The closed {@link org.xbib.io.sshd.common.session.Session}
     */
    default void sessionClosed(Session session) {
        // ignored
    }

    enum Event {
        KeyEstablished, Authenticated, KexCompleted
    }
}

package org.xbib.io.sshd.common.forward;

import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;
import org.xbib.io.sshd.common.util.net.SshdSocketAddress;

/**
 * A {@link ForwardingFilter} implementation that returns the same &quot;static&quot;
 * result for <U>all</U> the queries.
 */
public class StaticDecisionForwardingFilter extends AbstractLoggingBean implements ForwardingFilter {
    private final boolean acceptance;

    /**
     * @param acceptance The acceptance status for <U>all</U> the queries
     */
    public StaticDecisionForwardingFilter(boolean acceptance) {
        this.acceptance = acceptance;
    }

    public final boolean isAccepted() {
        return acceptance;
    }

    @Override
    public boolean canForwardAgent(Session session, String requestType) {
        return checkAcceptance(requestType, session, SshdSocketAddress.LOCALHOST_ADDRESS);
    }

    @Override
    public boolean canForwardX11(Session session, String requestType) {
        return checkAcceptance(requestType, session, SshdSocketAddress.LOCALHOST_ADDRESS);
    }

    @Override
    public boolean canListen(SshdSocketAddress address, Session session) {
        return checkAcceptance("tcpip-forward", session, address);
    }

    @Override
    public boolean canConnect(Type type, SshdSocketAddress address, Session session) {
        return checkAcceptance(type.getName(), session, address);
    }

    /**
     * @param request The SSH request that ultimately led to this filter being consulted
     * @param session The requesting {@link Session}
     * @param target  The request target - may be {@link SshdSocketAddress#LOCALHOST_ADDRESS}
     *                if no real target
     * @return The (static) {@link #isAccepted()} flag
     */
    protected boolean checkAcceptance(String request, Session session, SshdSocketAddress target) {
        boolean accepted = isAccepted();
        return accepted;
    }
}
package org.xbib.io.sshd.common.forward;

/**
 * A {@link ForwardingFilter} that rejects all requests.
 */
public class RejectAllForwardingFilter extends StaticDecisionForwardingFilter {
    public static final RejectAllForwardingFilter INSTANCE = new RejectAllForwardingFilter();

    public RejectAllForwardingFilter() {
        super(false);
    }
}
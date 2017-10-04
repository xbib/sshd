package org.xbib.io.sshd.server.forward;

import org.xbib.io.sshd.common.forward.ForwardingFilter;
import org.xbib.io.sshd.common.forward.StaticDecisionForwardingFilter;

/**
 * A {@link ForwardingFilter} that accepts all requests.
 */
public class AcceptAllForwardingFilter extends StaticDecisionForwardingFilter {
    public static final AcceptAllForwardingFilter INSTANCE = new AcceptAllForwardingFilter();

    public AcceptAllForwardingFilter() {
        super(true);
    }
}

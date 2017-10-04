package org.xbib.io.sshd.server.session;

import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.session.helpers.AbstractConnectionService;

/**
 * Server side <code>ssh-connection</code> service.
 */
public class ServerConnectionService
        extends AbstractConnectionService<AbstractServerSession>
        implements ServerSessionHolder {
    protected ServerConnectionService(AbstractServerSession s) throws SshException {
        super(s);

        if (!s.isAuthenticated()) {
            throw new SshException("Session is not authenticated");
        }
    }

    @Override
    public final ServerSession getServerSession() {
        return getSession();
    }
}

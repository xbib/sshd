package org.xbib.io.sshd.server.session;

import org.xbib.io.sshd.common.io.IoSession;
import org.xbib.io.sshd.common.session.helpers.AbstractSessionFactory;
import org.xbib.io.sshd.server.ServerFactoryManager;

/**
 * A factory of server sessions.
 * This class can be used as a way to customize the creation of server sessions.
 */
public class SessionFactory extends AbstractSessionFactory<ServerFactoryManager, ServerSessionImpl> {

    public SessionFactory(ServerFactoryManager server) {
        super(server);
    }

    public final ServerFactoryManager getServer() {
        return getFactoryManager();
    }

    @Override
    protected ServerSessionImpl doCreateSession(IoSession ioSession) throws Exception {
        return new ServerSessionImpl(getServer(), ioSession);
    }
}

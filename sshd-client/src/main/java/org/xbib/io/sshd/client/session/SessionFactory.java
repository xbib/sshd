package org.xbib.io.sshd.client.session;

import org.xbib.io.sshd.client.ClientFactoryManager;
import org.xbib.io.sshd.common.io.IoSession;
import org.xbib.io.sshd.common.session.helpers.AbstractSessionFactory;

/**
 * A factory of client sessions.
 * This class can be used as a way to customize the creation of client sessions.
 */
public class SessionFactory extends AbstractSessionFactory<ClientFactoryManager, ClientSessionImpl> {

    public SessionFactory(ClientFactoryManager client) {
        super(client);
    }

    public final ClientFactoryManager getClient() {
        return getFactoryManager();
    }

    @Override
    protected ClientSessionImpl doCreateSession(IoSession ioSession) throws Exception {
        return new ClientSessionImpl(getClient(), ioSession);
    }
}

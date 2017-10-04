package org.xbib.io.sshd.server.session;

import org.xbib.io.sshd.common.Service;
import org.xbib.io.sshd.common.ServiceFactory;
import org.xbib.io.sshd.common.forward.PortForwardingEventListener;
import org.xbib.io.sshd.common.session.AbstractConnectionServiceFactory;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.ValidateUtils;

import java.io.IOException;

/**
 *
 */
public class ServerConnectionServiceFactory extends AbstractConnectionServiceFactory implements ServiceFactory {
    public static final ServerConnectionServiceFactory INSTANCE = new ServerConnectionServiceFactory() {
        @Override
        public void addPortForwardingEventListener(PortForwardingEventListener listener) {
            throw new UnsupportedOperationException("addPortForwardingListener(" + listener + ") N/A on default instance");
        }

        @Override
        public void removePortForwardingEventListener(PortForwardingEventListener listener) {
            throw new UnsupportedOperationException("removePortForwardingEventListener(" + listener + ") N/A on default instance");
        }

        @Override
        public PortForwardingEventListener getPortForwardingEventListenerProxy() {
            return PortForwardingEventListener.EMPTY;
        }
    };

    public ServerConnectionServiceFactory() {
        super();
    }

    @Override
    public String getName() {
        return "ssh-connection";
    }

    @Override
    public Service create(Session session) throws IOException {
        org.xbib.io.sshd.server.session.AbstractServerSession abstractSession = ValidateUtils.checkInstanceOf(session, AbstractServerSession.class, "Not a server session: %s", session);
        org.xbib.io.sshd.server.session.ServerConnectionService service = new ServerConnectionService(abstractSession);
        service.addPortForwardingEventListenerManager(this);
        return service;
    }
}

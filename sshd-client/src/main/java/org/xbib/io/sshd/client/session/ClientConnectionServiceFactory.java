package org.xbib.io.sshd.client.session;

import org.xbib.io.sshd.common.Service;
import org.xbib.io.sshd.common.ServiceFactory;
import org.xbib.io.sshd.common.forward.PortForwardingEventListener;
import org.xbib.io.sshd.common.session.AbstractConnectionServiceFactory;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.ValidateUtils;

import java.io.IOException;

/**
 */
public class ClientConnectionServiceFactory extends AbstractConnectionServiceFactory implements ServiceFactory {
    public static final ClientConnectionServiceFactory INSTANCE = new ClientConnectionServiceFactory() {
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

    public ClientConnectionServiceFactory() {
        super();
    }

    @Override
    public String getName() {
        return "ssh-connection";
    }

    @Override
    public Service create(Session session) throws IOException {
        AbstractClientSession abstractSession =
                ValidateUtils.checkInstanceOf(session, AbstractClientSession.class, "Not a client session: %s", session);
        ClientConnectionService service = new ClientConnectionService(abstractSession);
        service.addPortForwardingEventListenerManager(this);
        return service;
    }
}
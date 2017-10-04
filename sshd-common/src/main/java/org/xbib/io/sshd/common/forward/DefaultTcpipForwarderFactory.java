package org.xbib.io.sshd.common.forward;

import org.xbib.io.sshd.common.session.ConnectionService;
import org.xbib.io.sshd.common.util.EventListenerUtils;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * The default {@link org.xbib.io.sshd.common.forward.TcpipForwarderFactory} implementation.
 */
public class DefaultTcpipForwarderFactory implements TcpipForwarderFactory, PortForwardingEventListenerManager {
    public static final DefaultTcpipForwarderFactory INSTANCE = new DefaultTcpipForwarderFactory() {
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

    private final Collection<PortForwardingEventListener> listeners = new CopyOnWriteArraySet<>();
    private final PortForwardingEventListener listenerProxy;

    public DefaultTcpipForwarderFactory() {
        listenerProxy = EventListenerUtils.proxyWrapper(PortForwardingEventListener.class, getClass().getClassLoader(), listeners);
    }

    @Override
    public PortForwardingEventListener getPortForwardingEventListenerProxy() {
        return listenerProxy;
    }

    @Override
    public void addPortForwardingEventListener(PortForwardingEventListener listener) {
        listeners.add(PortForwardingEventListener.validateListener(listener));
    }

    @Override
    public void removePortForwardingEventListener(PortForwardingEventListener listener) {
        if (listener == null) {
            return;
        }

        listeners.remove(PortForwardingEventListener.validateListener(listener));
    }

    @Override
    public TcpipForwarder create(ConnectionService service) {
        TcpipForwarder forwarder = new DefaultTcpipForwarder(service);
        forwarder.addPortForwardingEventListenerManager(this);
        return forwarder;
    }
}

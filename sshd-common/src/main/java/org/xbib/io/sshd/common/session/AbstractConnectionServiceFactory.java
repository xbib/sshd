package org.xbib.io.sshd.common.session;

import org.xbib.io.sshd.common.forward.PortForwardingEventListener;
import org.xbib.io.sshd.common.forward.PortForwardingEventListenerManager;
import org.xbib.io.sshd.common.util.EventListenerUtils;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 *
 */
public abstract class AbstractConnectionServiceFactory extends AbstractLoggingBean implements PortForwardingEventListenerManager {
    private final Collection<PortForwardingEventListener> listeners = new CopyOnWriteArraySet<>();
    private final PortForwardingEventListener listenerProxy;

    protected AbstractConnectionServiceFactory() {
        listenerProxy = EventListenerUtils.proxyWrapper(PortForwardingEventListener.class, getClass().getClassLoader(), listeners);
    }

    @Override
    public PortForwardingEventListener getPortForwardingEventListenerProxy() {
        return listenerProxy;
    }

    @Override
    public void addPortForwardingEventListener(PortForwardingEventListener listener) {
        listeners.add(Objects.requireNonNull(listener, "No listener to add"));
    }

    @Override
    public void removePortForwardingEventListener(PortForwardingEventListener listener) {
        if (listener == null) {
            return;
        }

        listeners.remove(listener);
    }
}

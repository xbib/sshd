package org.xbib.io.sshd.client.subsystem;

import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

/**
 *
 */
public abstract class AbstractSubsystemClient extends AbstractLoggingBean implements SubsystemClient {
    protected AbstractSubsystemClient() {
        super();
    }

    @Override
    public final ClientSession getSession() {
        return getClientSession();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "[name=" + getName()
                + ", session=" + getSession()
                + "]";
    }
}

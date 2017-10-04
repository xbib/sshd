package org.xbib.io.sshd.client.auth;

import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

import java.util.Objects;

/**
 *
 */
public abstract class AbstractUserAuth extends AbstractLoggingBean implements UserAuth {
    private final String name;
    private ClientSession clientSession;
    private String service;

    protected AbstractUserAuth(String name) {
        this.name = ValidateUtils.checkNotNullAndNotEmpty(name, "No name");
    }

    @Override
    public ClientSession getClientSession() {
        return clientSession;
    }

    @Override
    public ClientSession getSession() {
        return getClientSession();
    }

    @Override
    public final String getName() {
        return name;
    }

    public String getService() {
        return service;
    }

    @Override
    public void init(ClientSession session, String service) throws Exception {
        this.clientSession = Objects.requireNonNull(session, "No client session");
        this.service = ValidateUtils.checkNotNullAndNotEmpty(service, "No service");
    }

    @Override
    public boolean process(Buffer buffer) throws Exception {
        ClientSession session = getClientSession();
        String service = getService();
        if (buffer == null) {
            return sendAuthDataRequest(session, service);
        } else {
            return processAuthDataRequest(session, service, buffer);
        }
    }

    protected abstract boolean sendAuthDataRequest(ClientSession session, String service) throws Exception;

    protected abstract boolean processAuthDataRequest(ClientSession session, String service, Buffer buffer) throws Exception;

    @Override
    public void destroy() {
    }

    @Override
    public String toString() {
        return getName() + ": " + getSession() + "[" + getService() + "]";
    }
}

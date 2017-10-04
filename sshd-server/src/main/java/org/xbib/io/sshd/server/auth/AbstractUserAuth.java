package org.xbib.io.sshd.server.auth;

import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;
import org.xbib.io.sshd.server.session.ServerSession;

import java.util.Objects;

/**
 *
 */
public abstract class AbstractUserAuth extends AbstractLoggingBean implements UserAuth {
    private final String name;
    private ServerSession session;
    private String service;
    private String username;

    protected AbstractUserAuth(String name) {
        this.name = ValidateUtils.checkNotNullAndNotEmpty(name, "No name");
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public String getService() {
        return service;
    }

    @Override
    public ServerSession getServerSession() {
        return session;
    }

    @Override
    public ServerSession getSession() {
        return getServerSession();
    }

    @Override
    public Boolean auth(ServerSession session, String username, String service, Buffer buffer) throws Exception {
        this.session = Objects.requireNonNull(session, "No server session");
        this.username = username;
        this.service = service;
        return doAuth(buffer, true);
    }

    @Override
    public Boolean next(Buffer buffer) throws Exception {
        return doAuth(buffer, false);
    }

    @Override
    public void destroy() {
        // ignored
    }

    protected abstract Boolean doAuth(Buffer buffer, boolean init) throws Exception;

    @Override
    public String toString() {
        return getName() + ": " + getSession() + "[" + getService() + "]";
    }
}

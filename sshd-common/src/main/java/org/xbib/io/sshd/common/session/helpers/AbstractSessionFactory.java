package org.xbib.io.sshd.common.session.helpers;

import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.io.IoSession;

import java.util.Objects;

/**
 * An abstract base factory of sessions.
 *
 * @param <M> Type of {@link FactoryManager}
 * @param <S> Type of {@link AbstractSession}
 */
public abstract class AbstractSessionFactory<M extends FactoryManager, S extends AbstractSession> extends AbstractSessionIoHandler {
    private final M manager;

    protected AbstractSessionFactory(M manager) {
        this.manager = Objects.requireNonNull(manager, "No factory manager instance");
    }

    public M getFactoryManager() {
        return manager;
    }

    @Override
    protected S createSession(IoSession ioSession) throws Exception {
        return setupSession(doCreateSession(ioSession));
    }

    protected abstract S doCreateSession(IoSession ioSession) throws Exception;

    protected S setupSession(S session) throws Exception {
        return session;
    }
}

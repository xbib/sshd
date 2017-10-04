package org.xbib.io.sshd.common.session.helpers;

import org.xbib.io.sshd.common.RuntimeSshException;
import org.xbib.io.sshd.common.io.IoHandler;
import org.xbib.io.sshd.common.io.IoSession;
import org.xbib.io.sshd.common.util.Readable;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

/**
 *
 */
public abstract class AbstractSessionIoHandler extends AbstractLoggingBean implements IoHandler {
    protected AbstractSessionIoHandler() {
        super();
    }

    @Override
    public void sessionCreated(IoSession ioSession) throws Exception {
        ValidateUtils.checkNotNull(
                createSession(ioSession), "No session created for %s", ioSession);
    }

    @Override
    public void sessionClosed(IoSession ioSession) throws Exception {
        AbstractSession session = ValidateUtils.checkNotNull(
                AbstractSession.getSession(ioSession), "No abstract session to handle closure of %s", ioSession);
        session.close(true);
    }

    @Override
    public void exceptionCaught(IoSession ioSession, Throwable cause) throws Exception {
        AbstractSession session = AbstractSession.getSession(ioSession, true);
        if (session != null) {
            session.exceptionCaught(cause);
        } else {
            throw new IllegalStateException("No session available", cause);
        }
    }

    @Override
    public void messageReceived(IoSession ioSession, Readable message) throws Exception {
        AbstractSession session = ValidateUtils.checkNotNull(
                AbstractSession.getSession(ioSession), "No abstract session to handle incoming message for %s", ioSession);
        try {
            session.messageReceived(message);
        } catch (Error e) {
            throw new RuntimeSshException(e);
        }
    }

    protected abstract AbstractSession createSession(IoSession ioSession) throws Exception;
}

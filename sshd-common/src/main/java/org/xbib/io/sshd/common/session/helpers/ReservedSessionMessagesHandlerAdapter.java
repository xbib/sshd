package org.xbib.io.sshd.common.session.helpers;

import org.xbib.io.sshd.common.session.ReservedSessionMessagesHandler;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

/**
 * Delegates the main interface methods to specific ones after having
 * decoded each message buffer.
 */
public class ReservedSessionMessagesHandlerAdapter
        extends AbstractLoggingBean
        implements ReservedSessionMessagesHandler {
    public static final ReservedSessionMessagesHandlerAdapter DEFAULT = new ReservedSessionMessagesHandlerAdapter();

    public ReservedSessionMessagesHandlerAdapter() {
        super();
    }

    @Override
    public void handleIgnoreMessage(Session session, Buffer buffer) throws Exception {
        handleIgnoreMessage(session, buffer.getBytes(), buffer);
    }

    public void handleIgnoreMessage(Session session, byte[] data, Buffer buffer) throws Exception {
    }

    @Override
    public void handleDebugMessage(Session session, Buffer buffer) throws Exception {
        handleDebugMessage(session, buffer.getBoolean(), buffer.getString(), buffer.getString(), buffer);
    }

    public void handleDebugMessage(Session session, boolean display, String msg, String lang, Buffer buffer) throws Exception {
    }

    @Override
    public void handleUnimplementedMessage(Session session, Buffer buffer) throws Exception {
        handleUnimplementedMessage(session, buffer, buffer.getUInt());
    }

    public void handleUnimplementedMessage(Session session, Buffer buffer, long seqNo) throws Exception {
    }
}

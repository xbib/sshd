package org.xbib.io.sshd.server.session;

import org.xbib.io.sshd.common.io.IoSession;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.server.ServerFactoryManager;

/**
 * The default implementation for a {@link ServerSession}.
 */
public class ServerSessionImpl extends AbstractServerSession {
    public ServerSessionImpl(ServerFactoryManager server, IoSession ioSession) throws Exception {
        super(server, ioSession);
        signalSessionCreated(ioSession);

        String headerConfig = this.getString(ServerFactoryManager.SERVER_EXTRA_IDENTIFICATION_LINES);
        String[] headers = GenericUtils.split(headerConfig, ServerFactoryManager.SERVER_EXTRA_IDENT_LINES_SEPARATOR);
        sendServerIdentification(headers);
    }
}

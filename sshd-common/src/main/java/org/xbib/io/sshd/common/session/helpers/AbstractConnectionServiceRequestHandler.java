package org.xbib.io.sshd.common.session.helpers;

import org.xbib.io.sshd.common.session.ConnectionService;
import org.xbib.io.sshd.common.session.ConnectionServiceRequestHandler;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

/**
 *
 */
public abstract class AbstractConnectionServiceRequestHandler
        extends AbstractLoggingBean
        implements ConnectionServiceRequestHandler {

    protected AbstractConnectionServiceRequestHandler() {
        super();
    }

    @Override
    public Result process(ConnectionService connectionService, String request, boolean wantReply, Buffer buffer) throws Exception {
        return Result.Unsupported;
    }
}

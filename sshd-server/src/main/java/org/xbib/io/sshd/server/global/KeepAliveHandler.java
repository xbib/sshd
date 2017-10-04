package org.xbib.io.sshd.server.global;

import org.xbib.io.sshd.common.session.ConnectionService;
import org.xbib.io.sshd.common.session.helpers.AbstractConnectionServiceRequestHandler;
import org.xbib.io.sshd.common.util.buffer.Buffer;

/**
 * Handler for &quot;keepalive@xxx&quot; global request.
 */
public class KeepAliveHandler extends AbstractConnectionServiceRequestHandler {
    public static final KeepAliveHandler INSTANCE = new KeepAliveHandler();

    public KeepAliveHandler() {
        super();
    }

    @Override
    public Result process(ConnectionService connectionService, String request, boolean wantReply, Buffer buffer) throws Exception {
        if (request.startsWith("keepalive@")) {
            return Result.ReplyFailure;
        } else {
            return super.process(connectionService, request, wantReply, buffer);
        }
    }
}

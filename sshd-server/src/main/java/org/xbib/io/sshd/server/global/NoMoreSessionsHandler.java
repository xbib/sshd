package org.xbib.io.sshd.server.global;

import org.xbib.io.sshd.common.session.ConnectionService;
import org.xbib.io.sshd.common.session.helpers.AbstractConnectionServiceRequestHandler;
import org.xbib.io.sshd.common.util.buffer.Buffer;

/**
 * Handler for &quot;no-more-sessions@xxx&quot; global request.
 */
public class NoMoreSessionsHandler extends AbstractConnectionServiceRequestHandler {
    public static final NoMoreSessionsHandler INSTANCE = new NoMoreSessionsHandler();

    public NoMoreSessionsHandler() {
        super();
    }

    @Override
    public Result process(ConnectionService connectionService, String request, boolean wantReply, Buffer buffer) throws Exception {
        if (request.startsWith("no-more-sessions@")) {
            connectionService.setAllowMoreSessions(false);
            return Result.ReplyFailure;
        } else {
            return super.process(connectionService, request, wantReply, buffer);
        }
    }
}

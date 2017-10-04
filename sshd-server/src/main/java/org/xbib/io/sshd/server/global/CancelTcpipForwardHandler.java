package org.xbib.io.sshd.server.global;

import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.forward.TcpipForwarder;
import org.xbib.io.sshd.common.session.ConnectionService;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.session.helpers.AbstractConnectionServiceRequestHandler;
import org.xbib.io.sshd.common.util.Int2IntFunction;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.net.SshdSocketAddress;

import java.util.Objects;
import java.util.function.IntUnaryOperator;

/**
 * Handler for &quot;cancel-tcpip-forward&quot; global request.
 */
public class CancelTcpipForwardHandler extends AbstractConnectionServiceRequestHandler {
    public static final String REQUEST = "cancel-tcpip-forward";
    /**
     * Default growth factor function used to resize response buffers
     */
    public static final IntUnaryOperator RESPONSE_BUFFER_GROWTH_FACTOR = Int2IntFunction.add(Byte.SIZE);

    public static final CancelTcpipForwardHandler INSTANCE = new CancelTcpipForwardHandler();

    public CancelTcpipForwardHandler() {
        super();
    }

    @Override
    public Result process(ConnectionService connectionService, String request, boolean wantReply, Buffer buffer) throws Exception {
        if (!REQUEST.equals(request)) {
            return super.process(connectionService, request, wantReply, buffer);
        }

        String address = buffer.getString();
        int port = buffer.getInt();
        SshdSocketAddress socketAddress = new SshdSocketAddress(address, port);
        TcpipForwarder forwarder = Objects.requireNonNull(connectionService.getTcpipForwarder(), "No TCP/IP forwarder");
        forwarder.localPortForwardingCancelled(socketAddress);

        if (wantReply) {
            Session session = connectionService.getSession();
            buffer = session.createBuffer(SshConstants.SSH_MSG_REQUEST_SUCCESS, Integer.BYTES);
            buffer.putInt(port);
            session.writePacket(buffer);
        }

        return Result.Replied;
    }
}

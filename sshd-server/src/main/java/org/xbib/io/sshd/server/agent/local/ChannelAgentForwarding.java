package org.xbib.io.sshd.server.agent.local;

import org.xbib.io.sshd.common.agent.SshAgent;
import org.xbib.io.sshd.common.agent.SshAgentFactory;
import org.xbib.io.sshd.common.agent.AbstractAgentClient;
import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.channel.ChannelOutputStream;
import org.xbib.io.sshd.common.future.CloseFuture;
import org.xbib.io.sshd.common.future.DefaultOpenFuture;
import org.xbib.io.sshd.common.future.OpenFuture;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;
import org.xbib.io.sshd.server.channel.AbstractServerChannel;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * The client side channel that will receive requests forwards by the SSH server.
 */
public class ChannelAgentForwarding extends AbstractServerChannel {
    private OutputStream out;
    private SshAgent agent;
    private AgentClient client;

    public ChannelAgentForwarding() {
        super();
    }

    @Override
    protected OpenFuture doInit(Buffer buffer) {
        OpenFuture f = new DefaultOpenFuture(this, this);
        String changeEvent = "auth-agent";
        try {
            out = new ChannelOutputStream(this, getRemoteWindow(), SshConstants.SSH_MSG_CHANNEL_DATA, true);

            Session session = getSession();
            FactoryManager manager = Objects.requireNonNull(session.getFactoryManager(), "No factory manager");
            SshAgentFactory factory = Objects.requireNonNull(manager.getAgentFactory(), "No agent factory");
            agent = factory.createClient(manager);
            client = new AgentClient();

            signalChannelOpenSuccess();
            f.setOpened();
        } catch (Throwable t) {
            Throwable e = GenericUtils.peelException(t);
            changeEvent = e.getClass().getSimpleName();
            signalChannelOpenFailure(e);
            f.setException(e);
        } finally {
            notifyStateChanged(changeEvent);
        }
        return f;
    }

    private void closeImmediately0() {
        // We need to close the channel immediately to remove it from the
        // server session's channel table and *not* send a packet to the
        // client.  A notification was already sent by our caller, or will
        // be sent after we return.
        //
        super.close(true);
    }

    @Override
    public CloseFuture close(boolean immediately) {
        return super.close(immediately).addListener(sshFuture -> closeImmediately0());
    }

    @Override
    protected void doWriteData(byte[] data, int off, long len) throws IOException {
        ValidateUtils.checkTrue(len <= Integer.MAX_VALUE, "Data length exceeds int boundaries: %d", len);
        client.messageReceived(new ByteArrayBuffer(data, off, (int) len));
    }

    @Override
    protected void doWriteExtendedData(byte[] data, int off, long len) throws IOException {
        throw new UnsupportedOperationException("AgentForward channel does not support extended data");
    }

    @SuppressWarnings("synthetic-access")
    protected class AgentClient extends AbstractAgentClient {
        public AgentClient() {
            super(agent);
        }

        @Override
        protected void reply(Buffer buf) throws IOException {
            out.write(buf.array(), buf.rpos(), buf.available());
            out.flush();
        }
    }
}

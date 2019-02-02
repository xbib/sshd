package org.xbib.io.sshd.client.session;

import org.xbib.io.sshd.common.agent.AgentForwardSupport;
import org.xbib.io.sshd.client.ClientFactoryManager;
import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.io.AbstractIoWriteFuture;
import org.xbib.io.sshd.common.io.IoWriteFuture;
import org.xbib.io.sshd.common.session.helpers.AbstractConnectionService;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.x11.X11ForwardSupport;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Client side <code>ssh-connection</code> service.
 */
public class ClientConnectionService extends AbstractConnectionService<AbstractClientSession> implements ClientSessionHolder {
    public ClientConnectionService(AbstractClientSession s) {
        super(s);
    }

    @Override
    public final ClientSession getClientSession() {
        return getSession();
    }

    @Override
    public void start() {
        ClientSession session = getClientSession();
        if (!session.isAuthenticated()) {
            throw new IllegalStateException("Session is not authenticated");
        }
        startHeartBeat();
    }

    protected void startHeartBeat() {
        ClientSession session = getClientSession();
        long interval = session.getLongProperty(ClientFactoryManager.HEARTBEAT_INTERVAL, ClientFactoryManager.DEFAULT_HEARTBEAT_INTERVAL);
        if (interval > 0L) {
            FactoryManager manager = session.getFactoryManager();
            ScheduledExecutorService service = manager.getScheduledExecutorService();
            service.scheduleAtFixedRate(this::sendHeartBeat, interval, interval, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Sends a heartbeat message
     *
     * @return The {@link IoWriteFuture} that can be used to wait for the
     * message write completion
     */
    protected IoWriteFuture sendHeartBeat() {
        ClientSession session = getClientSession();
        String request = session.getStringProperty(ClientFactoryManager.HEARTBEAT_REQUEST, ClientFactoryManager.DEFAULT_KEEP_ALIVE_HEARTBEAT_STRING);
        try {
            Buffer buf = session.createBuffer(SshConstants.SSH_MSG_GLOBAL_REQUEST, request.length() + Byte.SIZE);
            buf.putString(request);
            buf.putBoolean(false);
            return session.writePacket(buf);
        } catch (IOException e) {

            final Throwable t = e;
            return new AbstractIoWriteFuture(request,null) {
                {
                    setValue(t);
                }
            };
        }
    }

    @Override
    public AgentForwardSupport getAgentForwardSupport() {
        throw new IllegalStateException("Server side operation");
    }

    @Override
    public X11ForwardSupport getX11ForwardSupport() {
        throw new IllegalStateException("Server side operation");
    }
}

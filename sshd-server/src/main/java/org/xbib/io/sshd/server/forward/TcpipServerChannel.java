package org.xbib.io.sshd.server.forward;

import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.RuntimeSshException;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.channel.ChannelOutputStream;
import org.xbib.io.sshd.common.channel.OpenChannelException;
import org.xbib.io.sshd.common.channel.Window;
import org.xbib.io.sshd.common.forward.ForwardingFilter;
import org.xbib.io.sshd.common.future.CloseFuture;
import org.xbib.io.sshd.common.future.DefaultOpenFuture;
import org.xbib.io.sshd.common.future.OpenFuture;
import org.xbib.io.sshd.common.io.IoConnectFuture;
import org.xbib.io.sshd.common.io.IoConnector;
import org.xbib.io.sshd.common.io.IoHandler;
import org.xbib.io.sshd.common.io.IoSession;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.Readable;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;
import org.xbib.io.sshd.common.util.net.SshdSocketAddress;
import org.xbib.io.sshd.common.util.threads.ThreadUtils;
import org.xbib.io.sshd.server.channel.AbstractServerChannel;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 *
 */
public class TcpipServerChannel extends AbstractServerChannel {

    private final ForwardingFilter.Type type;
    private IoConnector connector;
    private IoSession ioSession;
    private OutputStream out;

    public TcpipServerChannel(ForwardingFilter.Type type) {
        this.type = type;
    }

    public final ForwardingFilter.Type getChannelType() {
        return type;
    }

    @Override
    protected OpenFuture doInit(Buffer buffer) {
        String hostToConnect = buffer.getString();
        int portToConnect = buffer.getInt();
        String originatorIpAddress = buffer.getString();
        int originatorPort = buffer.getInt();

        final SshdSocketAddress address;
        switch (type) {
            case Direct:
                address = new SshdSocketAddress(hostToConnect, portToConnect);
                break;
            case Forwarded:
                address = service.getTcpipForwarder().getForwardedPort(portToConnect);
                break;
            default:
                throw new IllegalStateException("Unknown server channel type: " + type);
        }

        Session session = getSession();
        FactoryManager manager = Objects.requireNonNull(session.getFactoryManager(), "No factory manager");
        ForwardingFilter filter = manager.getTcpipForwardingFilter();
        OpenFuture f = new DefaultOpenFuture(this, this);
        try {
            if ((address == null) || (filter == null) || (!filter.canConnect(type, address, session))) {
                super.close(true);
                f.setException(new OpenChannelException(SshConstants.SSH_OPEN_ADMINISTRATIVELY_PROHIBITED, "Connection denied"));
                return f;
            }
        } catch (Error e) {
            throw new RuntimeSshException(e);
        }

        out = new ChannelOutputStream(this, getRemoteWindow(), SshConstants.SSH_MSG_CHANNEL_DATA, true);
        IoHandler handler = new IoHandler() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void messageReceived(IoSession session, Readable message) throws Exception {
                if (isClosing()) {
                } else {
                    Buffer buffer = new ByteArrayBuffer(message.available() + Long.SIZE, false);
                    buffer.putBuffer(message);
                    out.write(buffer.array(), buffer.rpos(), buffer.available());
                    out.flush();
                }
            }

            @Override
            public void sessionCreated(IoSession session) throws Exception {
                // ignored
            }

            @Override
            public void sessionClosed(IoSession session) throws Exception {
                close(false);
            }

            @Override
            public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
                close(true);
            }
        };

        connector = manager.getIoServiceFactory().createConnector(handler);
        IoConnectFuture future = connector.connect(address.toInetSocketAddress());
        future.addListener(future1 -> handleChannelConnectResult(f, future1));
        return f;
    }

    protected void handleChannelConnectResult(OpenFuture f, IoConnectFuture future) {
        try {
            if (future.isConnected()) {
                handleChannelOpenSuccess(f, future.getSession());
                return;
            }

            Throwable problem = GenericUtils.peelException(future.getException());
            if (problem != null) {
                handleChannelOpenFailure(f, problem);
            }
        } catch (RuntimeException t) {
            Throwable e = GenericUtils.peelException(t);
            signalChannelOpenFailure(e);
            try {
                f.setException(e);
            } finally {
                notifyStateChanged(e.getClass().getSimpleName());
            }
        }
    }

    protected void handleChannelOpenSuccess(OpenFuture f, IoSession session) {
        ioSession = session;

        String changeEvent = session.toString();
        try {
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
    }

    protected void handleChannelOpenFailure(OpenFuture f, Throwable problem) {
        signalChannelOpenFailure(problem);
        notifyStateChanged(problem.getClass().getSimpleName());
        closeImmediately0();

        if (problem instanceof ConnectException) {
            f.setException(new OpenChannelException(SshConstants.SSH_OPEN_CONNECT_FAILED, problem.getMessage(), problem));
        } else {
            f.setException(problem);
        }

    }

    private void closeImmediately0() {
        // We need to close the channel immediately to remove it from the
        // server session's channel table and *not* send a packet to the
        // client.  A notification was already sent by our caller, or will
        // be sent after we return.
        //
        super.close(true);

        // We also need to dispose of the connector, but unfortunately we
        // are being invoked by the connector thread or the connector's
        // own processor thread.  Disposing of the connector within either
        // causes deadlock.  Instead create a thread to dispose of the
        // connector in the background.

        ExecutorService service = getExecutorService();
        // allocate a temporary executor service if none provided
        final ExecutorService executors = (service == null)
                ? ThreadUtils.newSingleThreadExecutor("TcpIpServerChannel-ConnectorCleanup[" + getSession() + "]")
                : service;
        // shutdown the temporary executor service if had to create it
        final boolean shutdown = executors != service || isShutdownOnExit();
        executors.submit(() -> {
            try {
                connector.close(true);
            } finally {
                if (shutdown && !executors.isShutdown()) {
                    Collection<Runnable> runners = executors.shutdownNow();
                }
            }
        });
    }

    @Override
    public CloseFuture close(boolean immediately) {
        return super.close(immediately).addListener(sshFuture -> closeImmediately0());
    }

    @Override
    protected void doWriteData(byte[] data, int off, long len) throws IOException {
        ValidateUtils.checkTrue(len <= Integer.MAX_VALUE, "Data length exceeds int boundaries: %d", len);
        // Make sure we copy the data as the incoming buffer may be reused
        Buffer buf = ByteArrayBuffer.getCompactClone(data, off, (int) len);
        ioSession.writePacket(buf).addListener(future -> {
            if (future.isWritten()) {
                handleWriteDataSuccess(SshConstants.SSH_MSG_CHANNEL_DATA, buf.array(), 0, (int) len);
            } else {
                handleWriteDataFailure(SshConstants.SSH_MSG_CHANNEL_DATA, buf.array(), 0, (int) len, future.getException());
            }
        });
    }

    @Override
    protected void doWriteExtendedData(byte[] data, int off, long len) throws IOException {
        throw new UnsupportedOperationException(type + "Tcpip channel does not support extended data");
    }

    protected void handleWriteDataSuccess(byte cmd, byte[] data, int off, int len) {
        Session session = getSession();
        try {
            Window wLocal = getLocalWindow();
            wLocal.consumeAndCheck(len);
        } catch (Throwable e) {
            session.exceptionCaught(e);
        }
    }

    protected void handleWriteDataFailure(byte cmd, byte[] data, int off, int len, Throwable t) {
        Session session = getSession();
        session.exceptionCaught(t);
    }
}

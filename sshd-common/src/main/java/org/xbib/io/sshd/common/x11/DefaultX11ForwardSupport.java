package org.xbib.io.sshd.common.x11;

import org.xbib.io.sshd.common.Closeable;
import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.io.IoAcceptor;
import org.xbib.io.sshd.common.io.IoServiceFactory;
import org.xbib.io.sshd.common.io.IoSession;
import org.xbib.io.sshd.common.session.ConnectionService;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.OsUtils;
import org.xbib.io.sshd.common.util.Readable;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;
import org.xbib.io.sshd.common.util.closeable.AbstractInnerCloseable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Objects;

/**
 *
 */
public class DefaultX11ForwardSupport extends AbstractInnerCloseable implements X11ForwardSupport {

    private final ConnectionService service;
    private IoAcceptor acceptor;

    public DefaultX11ForwardSupport(ConnectionService service) {
        this.service = Objects.requireNonNull(service, "No connection service");
    }

    @Override
    public void close() throws IOException {
        close(true);
    }

    @Override
    protected Closeable getInnerCloseable() {
        return builder().close(acceptor).build();
    }

    // TODO consider reducing the 'synchronized' section to specific code locations rather than entire method
    @Override
    public synchronized String createDisplay(
            boolean singleConnection, String authenticationProtocol, String authenticationCookie, int screen)
            throws IOException {
        if (isClosed()) {
            throw new IllegalStateException("X11ForwardSupport is closed");
        }
        if (isClosing()) {
            throw new IllegalStateException("X11ForwardSupport is closing");
        }

        // only support non windows systems
        if (OsUtils.isWin32()) {
            return null;
        }

        Session session = Objects.requireNonNull(service.getSession(), "No session");
        if (acceptor == null) {
            FactoryManager manager = Objects.requireNonNull(session.getFactoryManager(), "No factory manager");
            IoServiceFactory factory = Objects.requireNonNull(manager.getIoServiceFactory(), "No I/O service factory");
            acceptor = factory.createAcceptor(this);
        }

        int minDisplayNumber = session.getIntProperty(X11_DISPLAY_OFFSET, DEFAULT_X11_DISPLAY_OFFSET);
        int maxDisplayNumber = session.getIntProperty(X11_MAX_DISPLAYS, DEFAULT_X11_MAX_DISPLAYS);
        int basePort = session.getIntProperty(X11_BASE_PORT, DEFAULT_X11_BASE_PORT);
        String bindHost = session.getStringProperty(X11_BIND_HOST, DEFAULT_X11_BIND_HOST);
        InetSocketAddress addr = null;

        // try until bind successful or max is reached
        for (int displayNumber = minDisplayNumber; displayNumber < maxDisplayNumber; displayNumber++) {
            int port = basePort + displayNumber;
            addr = new InetSocketAddress(bindHost, port);
            try {
                acceptor.bind(addr);
                break;
            } catch (BindException bindErr) {
                addr = null;
            }
        }

        if (addr == null) {
            Collection<SocketAddress> boundAddresses = acceptor.getBoundAddresses();
            if (GenericUtils.isEmpty(boundAddresses)) {
                close();
            } else {
            }

            return null;
        }

        int port = addr.getPort();
        int displayNumber = port - basePort;
        String authDisplay = "unix:" + displayNumber + "." + screen;
        try {
            Process p = new ProcessBuilder(XAUTH_COMMAND, "remove", authDisplay).start();
            int result = p.waitFor();
            if (result == 0) {
                p = new ProcessBuilder(XAUTH_COMMAND, "add", authDisplay, authenticationProtocol, authenticationCookie).start();
                result = p.waitFor();
            }

            if (result != 0) {
                throw new IllegalStateException("Bad " + XAUTH_COMMAND + " invocation result: " + result);
            }

            return bindHost + ":" + displayNumber + "." + screen;
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        ChannelForwardedX11 channel = new ChannelForwardedX11(session);
        session.setAttribute(ChannelForwardedX11.class, channel);
        this.service.registerChannel(channel);
        channel.open().verify(channel.getLongProperty(CHANNEL_OPEN_TIMEOUT_PROP, DEFAULT_CHANNEL_OPEN_TIMEOUT));
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        ChannelForwardedX11 channel = (ChannelForwardedX11) session.getAttribute(ChannelForwardedX11.class);
        if (channel != null) {
            channel.close(false);
        }
    }

    @Override
    public void messageReceived(IoSession session, Readable message) throws Exception {
        ChannelForwardedX11 channel = (ChannelForwardedX11) session.getAttribute(ChannelForwardedX11.class);
        Buffer buffer = new ByteArrayBuffer(message.available() + Long.SIZE, false);
        buffer.putBuffer(message);
        OutputStream outputStream = channel.getInvertedIn();
        outputStream.write(buffer.array(), buffer.rpos(), buffer.available());
        outputStream.flush();
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        session.close(false);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + service.getClass();
    }
}

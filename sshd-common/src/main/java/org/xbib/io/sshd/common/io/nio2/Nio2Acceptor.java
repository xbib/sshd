package org.xbib.io.sshd.common.io.nio2;

import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.future.CloseFuture;
import org.xbib.io.sshd.common.io.IoAcceptor;
import org.xbib.io.sshd.common.io.IoHandler;
import org.xbib.io.sshd.common.util.ValidateUtils;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class Nio2Acceptor extends Nio2Service implements IoAcceptor {
    protected final Map<SocketAddress, AsynchronousServerSocketChannel> channels = new ConcurrentHashMap<>();
    private int backlog = DEFAULT_BACKLOG;

    public Nio2Acceptor(FactoryManager manager, IoHandler handler, AsynchronousChannelGroup group) {
        super(manager, handler, group);
        backlog = manager.getIntProperty(FactoryManager.SOCKET_BACKLOG, DEFAULT_BACKLOG);
    }

    @Override
    public void bind(Collection<? extends SocketAddress> addresses) throws IOException {
        AsynchronousChannelGroup group = getChannelGroup();
        for (SocketAddress address : addresses) {

            AsynchronousServerSocketChannel socket =
                    setSocketOptions(openAsynchronousServerSocketChannel(address, group));
            socket.bind(address, backlog);
            SocketAddress local = socket.getLocalAddress();
            channels.put(local, socket);

            CompletionHandler<AsynchronousSocketChannel, ? super SocketAddress> handler =
                    ValidateUtils.checkNotNull(createSocketCompletionHandler(channels, socket),
                            "No completion handler created for address=%s",
                            address);
            socket.accept(local, handler);
        }
    }

    protected AsynchronousServerSocketChannel openAsynchronousServerSocketChannel(
            SocketAddress address, AsynchronousChannelGroup group) throws IOException {
        return AsynchronousServerSocketChannel.open(group);
    }

    protected CompletionHandler<AsynchronousSocketChannel, ? super SocketAddress> createSocketCompletionHandler(
            Map<SocketAddress, AsynchronousServerSocketChannel> channelsMap, AsynchronousServerSocketChannel socket) throws IOException {
        return new AcceptCompletionHandler(socket);
    }

    @Override
    public void bind(SocketAddress address) throws IOException {
        bind(Collections.singleton(address));
    }

    @Override
    public void unbind() {
        unbind(getBoundAddresses());
    }

    @Override
    public void unbind(Collection<? extends SocketAddress> addresses) {
        for (SocketAddress address : addresses) {
            AsynchronousServerSocketChannel channel = channels.remove(address);
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                }
            } else {
            }
        }
    }

    @Override
    public void unbind(SocketAddress address) {
        unbind(Collections.singleton(address));
    }

    @Override
    public Set<SocketAddress> getBoundAddresses() {
        return new HashSet<>(channels.keySet());
    }

    @Override
    public CloseFuture close(boolean immediately) {
        unbind();
        return super.close(immediately);
    }

    @Override
    public void doCloseImmediately() {
        for (SocketAddress address : channels.keySet()) {
            try {
                channels.get(address).close();
            } catch (IOException e) {
            }
        }
        super.doCloseImmediately();
    }

    protected class AcceptCompletionHandler extends Nio2CompletionHandler<AsynchronousSocketChannel, SocketAddress> {
        protected final AsynchronousServerSocketChannel socket;

        AcceptCompletionHandler(AsynchronousServerSocketChannel socket) {
            this.socket = socket;
        }

        @Override
        @SuppressWarnings("synthetic-access")
        protected void onCompleted(AsynchronousSocketChannel result, SocketAddress address) {
            // Verify that the address has not been unbound
            if (!channels.containsKey(address)) {
                return;
            }

            org.xbib.io.sshd.common.io.nio2.Nio2Session session = null;
            try {
                // Create a session
                IoHandler handler = getIoHandler();
                setSocketOptions(result);
                session = Objects.requireNonNull(createSession(Nio2Acceptor.this, address, result, handler), "No NIO2 session created");
                handler.sessionCreated(session);
                sessions.put(session.getId(), session);
                session.startReading();
            } catch (Throwable exc) {
                failed(exc, address);

                // fail fast the accepted connection
                if (session != null) {
                    try {
                        session.close();
                    } catch (Throwable t) {
                    }
                }
            }

            try {
                // Accept new connections
                socket.accept(address, this);
            } catch (Throwable exc) {
                failed(exc, address);
            }
        }

        @SuppressWarnings("synthetic-access")
        protected org.xbib.io.sshd.common.io.nio2.Nio2Session createSession(Nio2Acceptor acceptor, SocketAddress address, AsynchronousSocketChannel channel, IoHandler handler) throws Throwable {
            return new Nio2Session(acceptor, getFactoryManager(), handler, channel);
        }

        @Override
        @SuppressWarnings("synthetic-access")
        protected void onFailed(final Throwable exc, final SocketAddress address) {
            if (channels.containsKey(address) && !disposing.get()) {
            }
        }
    }
}

package org.xbib.io.sshd.common.io.nio2;

import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.future.DefaultSshFuture;
import org.xbib.io.sshd.common.io.IoConnectFuture;
import org.xbib.io.sshd.common.io.IoConnector;
import org.xbib.io.sshd.common.io.IoHandler;
import org.xbib.io.sshd.common.io.IoSession;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;

/**
 *
 */
public class Nio2Connector extends Nio2Service implements IoConnector {
    public Nio2Connector(FactoryManager manager, IoHandler handler, AsynchronousChannelGroup group) {
        super(manager, handler, group);
    }

    @Override
    public IoConnectFuture connect(SocketAddress address) {

        IoConnectFuture future = new DefaultIoConnectFuture(null);
        try {
            AsynchronousChannelGroup group = getChannelGroup();
            AsynchronousSocketChannel socket =
                    setSocketOptions(openAsynchronousSocketChannel(address, group));
            Nio2CompletionHandler<Void, Object> completionHandler =
                    ValidateUtils.checkNotNull(createConnectionCompletionHandler(future, socket, getFactoryManager(), getIoHandler()),
                            "No connection completion handler created for %s",
                            address);
            socket.connect(address, null, completionHandler);
        } catch (Throwable exc) {
            Throwable t = GenericUtils.peelException(exc);
            future.setException(t);
        }
        return future;
    }

    protected AsynchronousSocketChannel openAsynchronousSocketChannel(
            SocketAddress address, AsynchronousChannelGroup group) throws IOException {
        return AsynchronousSocketChannel.open(group);
    }

    protected Nio2CompletionHandler<Void, Object> createConnectionCompletionHandler(
            final IoConnectFuture future, final AsynchronousSocketChannel socket, final FactoryManager manager, final IoHandler handler) {
        return new Nio2CompletionHandler<Void, Object>() {
            @Override
            @SuppressWarnings("synthetic-access")
            protected void onCompleted(Void result, Object attachment) {
                try {
                    Nio2Session session = createSession(manager, handler, socket);
                    handler.sessionCreated(session);
                    sessions.put(session.getId(), session);
                    future.setSession(session);
                    session.startReading();
                } catch (Throwable exc) {
                    Throwable t = GenericUtils.peelException(exc);
                    try {
                        socket.close();
                    } catch (IOException err) {
                    }
                    future.setException(t);
                }
            }

            @Override
            protected void onFailed(final Throwable exc, final Object attachment) {
                future.setException(exc);
            }
        };
    }

    protected org.xbib.io.sshd.common.io.nio2.Nio2Session createSession(FactoryManager manager, IoHandler handler, AsynchronousSocketChannel socket) throws Throwable {
        return new Nio2Session(this, manager, handler, socket);
    }

    public static class DefaultIoConnectFuture extends DefaultSshFuture<IoConnectFuture> implements IoConnectFuture {
        public DefaultIoConnectFuture(Object lock) {
            super(lock);
        }

        @Override
        public IoSession getSession() {
            Object v = getValue();
            return v instanceof IoSession ? (IoSession) v : null;
        }

        @Override
        public void setSession(IoSession session) {
            setValue(session);
        }

        @Override
        public Throwable getException() {
            Object v = getValue();
            return v instanceof Throwable ? (Throwable) v : null;
        }

        @Override
        public void setException(Throwable exception) {
            setValue(exception);
        }

        @Override
        public boolean isConnected() {
            return getValue() instanceof IoSession;
        }
    }
}

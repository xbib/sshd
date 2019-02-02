package org.xbib.io.sshd.common.io.nio2;

import org.xbib.io.sshd.common.Closeable;
import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.FactoryManagerHolder;
import org.xbib.io.sshd.common.PropertyResolver;
import org.xbib.io.sshd.common.io.IoHandler;
import org.xbib.io.sshd.common.io.IoService;
import org.xbib.io.sshd.common.io.IoSession;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.Pair;
import org.xbib.io.sshd.common.util.closeable.AbstractInnerCloseable;

import java.io.IOException;
import java.net.SocketOption;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.NetworkChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public abstract class Nio2Service extends AbstractInnerCloseable implements IoService, FactoryManagerHolder {
    public static final Map<String, Pair<SocketOption<?>, Object>> CONFIGURABLE_OPTIONS =
            Collections.unmodifiableMap(new LinkedHashMap<String, Pair<SocketOption<?>, Object>>() {
                // Not serializing it
                private static final long serialVersionUID = 1L;

                {
                    put(FactoryManager.SOCKET_KEEPALIVE, new Pair<>(StandardSocketOptions.SO_KEEPALIVE, null));
                    put(FactoryManager.SOCKET_LINGER, new Pair<>(StandardSocketOptions.SO_LINGER, null));
                    put(FactoryManager.SOCKET_RCVBUF, new Pair<>(StandardSocketOptions.SO_RCVBUF, null));
                    put(FactoryManager.SOCKET_REUSEADDR, new Pair<>(StandardSocketOptions.SO_REUSEADDR, DEFAULT_REUSE_ADDRESS));
                    put(FactoryManager.SOCKET_SNDBUF, new Pair<>(StandardSocketOptions.SO_SNDBUF, null));
                    put(FactoryManager.TCP_NODELAY, new Pair<>(StandardSocketOptions.TCP_NODELAY, null));
                }
            });

    protected final Map<Long, IoSession> sessions;
    protected final AtomicBoolean disposing = new AtomicBoolean();
    private final FactoryManager manager;
    private final IoHandler handler;
    private final AsynchronousChannelGroup group;

    protected Nio2Service(FactoryManager manager, IoHandler handler, AsynchronousChannelGroup group) {
        this.manager = Objects.requireNonNull(manager, "No factory manager provided");
        this.handler = Objects.requireNonNull(handler, "No I/O handler provided");
        this.group = Objects.requireNonNull(group, "No async. channel group provided");
        this.sessions = new ConcurrentHashMap<>();
    }

    protected AsynchronousChannelGroup getChannelGroup() {
        return group;
    }

    @Override
    public FactoryManager getFactoryManager() {
        return manager;
    }

    public IoHandler getIoHandler() {
        return handler;
    }

    public void dispose() {
        try {
            long maxWait = Closeable.getMaxCloseWaitTime(getFactoryManager());
            boolean successful = close(true).await(maxWait);
            if (!successful) {
                throw new SocketTimeoutException("Failed to receive closure confirmation within " + maxWait + " millis");
            }
        } catch (IOException e) {
        }
    }

    @Override
    protected Closeable getInnerCloseable() {
        return builder().parallel(toString(), sessions.values()).build();
    }

    @Override
    public Map<Long, IoSession> getManagedSessions() {
        return Collections.unmodifiableMap(sessions);
    }

    public void sessionClosed(Nio2Session session) {
        sessions.remove(session.getId());
    }

    @SuppressWarnings("unchecked")
    protected <S extends NetworkChannel> S setSocketOptions(S socket) throws IOException {
        Collection<? extends SocketOption<?>> supported = socket.supportedOptions();
        if (GenericUtils.isEmpty(supported)) {
            return socket;
        }

        for (Map.Entry<String, Pair<SocketOption<?>, Object>> ce : CONFIGURABLE_OPTIONS.entrySet()) {
            String property = ce.getKey();
            Pair<SocketOption<?>, Object> defConfig = ce.getValue();
            @SuppressWarnings("rawtypes")
            SocketOption option = defConfig.getKey();
            setOption(socket, property, option, defConfig.getValue());
        }

        return socket;
    }

    protected <T> boolean setOption(NetworkChannel socket, String property, SocketOption<T> option, T defaultValue) throws IOException {
        PropertyResolver manager = getFactoryManager();
        String valStr = manager.getString(property);
        T val = defaultValue;
        if (!GenericUtils.isEmpty(valStr)) {
            Class<T> type = option.type();
            if (type == Integer.class) {
                val = type.cast(Integer.valueOf(valStr));
            } else if (type == Boolean.class) {
                val = type.cast(Boolean.valueOf(valStr));
            } else {
                throw new IllegalStateException("Unsupported socket option type (" + type + ") " + property + "=" + valStr);
            }
        }

        if (val == null) {
            return false;
        }

        Collection<? extends SocketOption<?>> supported = socket.supportedOptions();
        if (GenericUtils.isEmpty(supported) || (!supported.contains(option))) {
            return false;
        }

        try {
            socket.setOption(option, val);
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }
}

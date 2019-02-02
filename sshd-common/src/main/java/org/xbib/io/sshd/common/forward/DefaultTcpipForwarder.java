package org.xbib.io.sshd.common.forward;

import org.xbib.io.sshd.common.Closeable;
import org.xbib.io.sshd.common.Factory;
import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.RuntimeSshException;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.channel.ClientChannelEvent;
import org.xbib.io.sshd.common.io.IoAcceptor;
import org.xbib.io.sshd.common.io.IoHandler;
import org.xbib.io.sshd.common.io.IoHandlerFactory;
import org.xbib.io.sshd.common.io.IoServiceFactory;
import org.xbib.io.sshd.common.io.IoSession;
import org.xbib.io.sshd.common.session.ConnectionService;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.session.SessionHolder;
import org.xbib.io.sshd.common.util.EventListenerUtils;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.Invoker;
import org.xbib.io.sshd.common.util.Readable;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;
import org.xbib.io.sshd.common.util.closeable.AbstractInnerCloseable;
import org.xbib.io.sshd.common.util.net.SshdSocketAddress;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * Requests a &quot;tcpip-forward&quot; action.
 */
public class DefaultTcpipForwarder
        extends AbstractInnerCloseable
        implements TcpipForwarder, SessionHolder<Session>, PortForwardingEventListenerManager {

    /**
     * Used to configure the timeout (milliseconds) for receiving a response
     * for the forwarding request
     *
     * @see #DEFAULT_FORWARD_REQUEST_TIMEOUT
     */
    public static final String FORWARD_REQUEST_TIMEOUT = "tcpip-forward-request-timeout";

    /**
     * Default value for {@link #FORWARD_REQUEST_TIMEOUT} if none specified
     */
    public static final long DEFAULT_FORWARD_REQUEST_TIMEOUT = TimeUnit.SECONDS.toMillis(15L);

    public static final Set<ClientChannelEvent> STATIC_IO_MSG_RECEIVED_EVENTS =
            Collections.unmodifiableSet(EnumSet.of(ClientChannelEvent.OPENED, ClientChannelEvent.CLOSED));

    private final ConnectionService service;
    private final IoHandlerFactory socksProxyIoHandlerFactory = () -> new SocksProxy(getConnectionService());
    private final Session sessionInstance;
    private final Map<Integer, SshdSocketAddress> localToRemote = new HashMap<>();
    private final Map<Integer, SshdSocketAddress> remoteToLocal = new HashMap<>();
    private final Map<Integer, SocksProxy> dynamicLocal = new HashMap<>();
    private final Set<LocalForwardingEntry> localForwards = new HashSet<>();
    private final IoHandlerFactory staticIoHandlerFactory = StaticIoHandler::new;
    private final Collection<PortForwardingEventListener> listeners = new CopyOnWriteArraySet<>();
    private final Collection<PortForwardingEventListenerManager> managersHolder = new CopyOnWriteArraySet<>();
    private final PortForwardingEventListener listenerProxy;

    private IoAcceptor acceptor;

    public DefaultTcpipForwarder(ConnectionService service) {
        this.service = Objects.requireNonNull(service, "No connection service");
        this.sessionInstance = Objects.requireNonNull(service.getSession(), "No session");
        this.listenerProxy = EventListenerUtils.proxyWrapper(PortForwardingEventListener.class, getClass().getClassLoader(), listeners);
    }

    @Override
    public PortForwardingEventListener getPortForwardingEventListenerProxy() {
        return listenerProxy;
    }

    @Override
    public void addPortForwardingEventListener(PortForwardingEventListener listener) {
        listeners.add(PortForwardingEventListener.validateListener(listener));
    }

    @Override
    public void removePortForwardingEventListener(PortForwardingEventListener listener) {
        if (listener == null) {
            return;
        }

        listeners.remove(PortForwardingEventListener.validateListener(listener));
    }

    @Override
    public Collection<PortForwardingEventListenerManager> getRegisteredManagers() {
        return managersHolder.isEmpty() ? Collections.emptyList() : new ArrayList<>(managersHolder);
    }

    @Override
    public boolean addPortForwardingEventListenerManager(PortForwardingEventListenerManager manager) {
        return managersHolder.add(Objects.requireNonNull(manager, "No manager"));
    }

    @Override
    public boolean removePortForwardingEventListenerManager(PortForwardingEventListenerManager manager) {
        if (manager == null) {
            return false;
        }

        return managersHolder.remove(manager);
    }

    @Override
    public Session getSession() {
        return sessionInstance;
    }

    public final ConnectionService getConnectionService() {
        return service;
    }

    protected Collection<PortForwardingEventListener> getDefaultListeners() {
        Collection<PortForwardingEventListener> defaultListeners = new ArrayList<>();
        defaultListeners.add(getPortForwardingEventListenerProxy());

        Session session = getSession();
        PortForwardingEventListener l = session.getPortForwardingEventListenerProxy();
        if (l != null) {
            defaultListeners.add(l);
        }

        FactoryManager manager = (session == null) ? null : session.getFactoryManager();
        l = (manager == null) ? null : manager.getPortForwardingEventListenerProxy();
        if (l != null) {
            defaultListeners.add(l);
        }

        return defaultListeners;
    }

    //
    // TcpIpForwarder implementation
    //

    @Override
    public synchronized SshdSocketAddress startLocalPortForwarding(SshdSocketAddress local, SshdSocketAddress remote) throws IOException {
        Objects.requireNonNull(local, "Local address is null");
        ValidateUtils.checkTrue(local.getPort() >= 0, "Invalid local port: %s", local);
        Objects.requireNonNull(remote, "Remote address is null");

        if (isClosed()) {
            throw new IllegalStateException("TcpipForwarder is closed");
        }
        if (isClosing()) {
            throw new IllegalStateException("TcpipForwarder is closing");
        }

        InetSocketAddress bound;
        int port;
        signalEstablishingExplicitTunnel(local, remote, true);
        try {
            bound = doBind(local, staticIoHandlerFactory);
            port = bound.getPort();
            SshdSocketAddress prev;
            synchronized (localToRemote) {
                prev = localToRemote.put(port, remote);
            }

            if (prev != null) {
                throw new IOException("Multiple local port forwarding bindings on port=" + port + ": current=" + remote + ", previous=" + prev);
            }
        } catch (IOException | RuntimeException e) {
            try {
                stopLocalPortForwarding(local);
            } catch (IOException | RuntimeException err) {
                e.addSuppressed(err);
            }
            signalEstablishedExplicitTunnel(local, remote, true, null, e);
            throw e;
        }

        try {
            SshdSocketAddress result = new SshdSocketAddress(bound.getHostString(), port);
            signalEstablishedExplicitTunnel(local, remote, true, result, null);
            return result;
        } catch (IOException | RuntimeException e) {
            stopLocalPortForwarding(local);
            throw e;
        }
    }

    @Override
    public synchronized void stopLocalPortForwarding(SshdSocketAddress local) throws IOException {
        Objects.requireNonNull(local, "Local address is null");

        SshdSocketAddress bound;
        synchronized (localToRemote) {
            bound = localToRemote.remove(local.getPort());
        }

        if ((bound != null) && (acceptor != null)) {

            signalTearingDownExplicitTunnel(bound, true);
            try {
                acceptor.unbind(bound.toInetSocketAddress());
            } catch (RuntimeException e) {
                signalTornDownExplicitTunnel(bound, true, e);
                throw e;
            }

            signalTornDownExplicitTunnel(bound, true, null);
        } else {
        }
    }

    @Override
    public synchronized SshdSocketAddress startRemotePortForwarding(SshdSocketAddress remote, SshdSocketAddress local) throws IOException {
        Objects.requireNonNull(local, "Local address is null");
        Objects.requireNonNull(remote, "Remote address is null");

        String remoteHost = remote.getHostName();
        int remotePort = remote.getPort();
        Session session = getSession();
        Buffer buffer = session.createBuffer(SshConstants.SSH_MSG_GLOBAL_REQUEST, remoteHost.length() + Long.SIZE);
        buffer.putString("tcpip-forward");
        buffer.putBoolean(true);    // want reply
        buffer.putString(remoteHost);
        buffer.putInt(remotePort);

        long timeout = session.getLongProperty(FORWARD_REQUEST_TIMEOUT, DEFAULT_FORWARD_REQUEST_TIMEOUT);
        Buffer result;
        int port;
        signalEstablishingExplicitTunnel(local, remote, false);
        try {
            result = session.request("tcpip-forward", buffer, timeout, TimeUnit.MILLISECONDS);
            if (result == null) {
                throw new SshException("Tcpip forwarding request denied by server");
            }
            port = (remotePort == 0) ? result.getInt() : remote.getPort();
            // TODO: Is it really safe to only store the local address after the request ?
            SshdSocketAddress prev;
            synchronized (remoteToLocal) {
                prev = remoteToLocal.put(port, local);
            }

            if (prev != null) {
                throw new IOException("Multiple remote port forwarding bindings on port=" + port + ": current=" + remote + ", previous=" + prev);
            }
        } catch (IOException | RuntimeException e) {
            try {
                stopRemotePortForwarding(remote);
            } catch (IOException | RuntimeException err) {
                e.addSuppressed(err);
            }
            signalEstablishedExplicitTunnel(local, remote, false, null, e);
            throw e;
        }

        try {
            SshdSocketAddress bound = new SshdSocketAddress(remoteHost, port);
            signalEstablishedExplicitTunnel(local, remote, false, bound, null);
            return bound;
        } catch (IOException | RuntimeException e) {
            stopRemotePortForwarding(remote);
            throw e;
        }
    }

    @Override
    public synchronized void stopRemotePortForwarding(SshdSocketAddress remote) throws IOException {
        SshdSocketAddress bound;
        synchronized (remoteToLocal) {
            bound = remoteToLocal.remove(remote.getPort());
        }

        if (bound != null) {
            String remoteHost = remote.getHostName();
            Session session = getSession();
            Buffer buffer = session.createBuffer(SshConstants.SSH_MSG_GLOBAL_REQUEST, remoteHost.length() + Long.SIZE);
            buffer.putString("cancel-tcpip-forward");
            buffer.putBoolean(false);   // want reply
            buffer.putString(remoteHost);
            buffer.putInt(remote.getPort());

            signalTearingDownExplicitTunnel(bound, false);
            try {
                session.writePacket(buffer);
            } catch (IOException | RuntimeException e) {
                signalTornDownExplicitTunnel(bound, false, e);
                throw e;
            }

            signalTornDownExplicitTunnel(bound, false, null);
        } else {
        }
    }

    protected void signalTearingDownExplicitTunnel(SshdSocketAddress boundAddress, boolean localForwarding) throws IOException {
        try {
            invokePortEventListenerSignaller(l -> {
                signalTearingDownExplicitTunnel(l, boundAddress, localForwarding);
                return null;
            });
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else if (t instanceof IOException) {
                throw (IOException) t;
            } else {
                throw new IOException("Failed (" + t.getClass().getSimpleName() + ")"
                        + " to signal tearing down explicit tunnel for local=" + localForwarding
                        + " on bound=" + boundAddress, t);
            }
        }
    }

    protected void signalTearingDownExplicitTunnel(
            PortForwardingEventListener listener, SshdSocketAddress boundAddress, boolean localForwarding)
            throws IOException {
        if (listener == null) {
            return;
        }

        listener.tearingDownExplicitTunnel(getSession(), boundAddress, localForwarding);
    }

    protected void signalTornDownExplicitTunnel(SshdSocketAddress boundAddress, boolean localForwarding, Throwable reason) throws IOException {
        try {
            invokePortEventListenerSignaller(l -> {
                signalTornDownExplicitTunnel(l, boundAddress, localForwarding, reason);
                return null;
            });
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else if (t instanceof IOException) {
                throw (IOException) t;
            } else {
                throw new IOException("Failed (" + t.getClass().getSimpleName() + ")"
                        + " to signal torn down explicit tunnel local=" + localForwarding
                        + " on bound=" + boundAddress, t);
            }
        }
    }

    protected void signalTornDownExplicitTunnel(
            PortForwardingEventListener listener, SshdSocketAddress boundAddress, boolean localForwarding, Throwable reason)
            throws IOException {
        if (listener == null) {
            return;
        }

        listener.tornDownExplicitTunnel(getSession(), boundAddress, localForwarding, reason);
    }

    @Override
    public synchronized SshdSocketAddress startDynamicPortForwarding(SshdSocketAddress local) throws IOException {
        Objects.requireNonNull(local, "Local address is null");
        ValidateUtils.checkTrue(local.getPort() >= 0, "Invalid local port: %s", local);

        if (isClosed()) {
            throw new IllegalStateException("TcpipForwarder is closed");
        }
        if (isClosing()) {
            throw new IllegalStateException("TcpipForwarder is closing");
        }

        SocksProxy socksProxy = new SocksProxy(service);
        SocksProxy prev;
        InetSocketAddress bound;
        int port;
        signalEstablishingDynamicTunnel(local);
        try {
            bound = doBind(local, socksProxyIoHandlerFactory);
            port = bound.getPort();
            synchronized (dynamicLocal) {
                prev = dynamicLocal.put(port, socksProxy);
            }

            if (prev != null) {
                throw new IOException("Multiple dynamic port mappings found for port=" + port + ": current=" + socksProxy + ", previous=" + prev);
            }
        } catch (IOException | RuntimeException e) {
            try {
                stopDynamicPortForwarding(local);
            } catch (IOException | RuntimeException err) {
                e.addSuppressed(err);
            }
            signalEstablishedDynamicTunnel(local, null, e);
            throw e;
        }

        try {
            SshdSocketAddress result = new SshdSocketAddress(bound.getHostString(), port);
            signalEstablishedDynamicTunnel(local, result, null);
            return result;
        } catch (IOException | RuntimeException e) {
            stopDynamicPortForwarding(local);
            throw e;
        }
    }

    protected void signalEstablishedDynamicTunnel(
            SshdSocketAddress local, SshdSocketAddress boundAddress, Throwable reason)
            throws IOException {
        try {
            invokePortEventListenerSignaller(l -> {
                signalEstablishedDynamicTunnel(l, local, boundAddress, reason);
                return null;
            });
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else if (t instanceof IOException) {
                throw (IOException) t;
            } else {
                throw new IOException("Failed (" + t.getClass().getSimpleName() + ")"
                        + " to signal establishing dynamic tunnel for local=" + local
                        + " on bound=" + boundAddress, t);
            }
        }
    }

    protected void signalEstablishedDynamicTunnel(PortForwardingEventListener listener,
                                                  SshdSocketAddress local, SshdSocketAddress boundAddress, Throwable reason)
            throws IOException {
        if (listener == null) {
            return;
        }

        listener.establishedDynamicTunnel(getSession(), local, boundAddress, reason);
    }

    protected void signalEstablishingDynamicTunnel(SshdSocketAddress local) throws IOException {
        try {
            invokePortEventListenerSignaller(l -> {
                signalEstablishingDynamicTunnel(l, local);
                return null;
            });
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else if (t instanceof IOException) {
                throw (IOException) t;
            } else {
                throw new IOException("Failed (" + t.getClass().getSimpleName() + ")"
                        + " to signal establishing dynamic tunnel for local=" + local, t);
            }
        }
    }

    protected void signalEstablishingDynamicTunnel(PortForwardingEventListener listener, SshdSocketAddress local) throws IOException {
        if (listener == null) {
            return;
        }

        listener.establishingDynamicTunnel(getSession(), local);
    }

    @Override
    public synchronized void stopDynamicPortForwarding(SshdSocketAddress local) throws IOException {
        SocksProxy obj;
        synchronized (dynamicLocal) {
            obj = dynamicLocal.remove(local.getPort());
        }

        if (obj != null) {
            signalTearingDownDynamicTunnel(local);
            try {
                obj.close(true);
                acceptor.unbind(local.toInetSocketAddress());
            } catch (RuntimeException e) {
                signalTornDownDynamicTunnel(local, e);
                throw e;
            }

            signalTornDownDynamicTunnel(local, null);
        } else {
        }
    }

    protected void signalTearingDownDynamicTunnel(SshdSocketAddress address) throws IOException {
        try {
            invokePortEventListenerSignaller(l -> {
                signalTearingDownDynamicTunnel(l, address);
                return null;
            });
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else if (t instanceof IOException) {
                throw (IOException) t;
            } else {
                throw new IOException("Failed (" + t.getClass().getSimpleName() + ")"
                        + " to signal tearing down dynamic tunnel for address=" + address, t);
            }
        }
    }

    protected void signalTearingDownDynamicTunnel(PortForwardingEventListener listener, SshdSocketAddress address) throws IOException {
        if (listener == null) {
            return;
        }

        listener.tearingDownDynamicTunnel(getSession(), address);
    }

    protected void signalTornDownDynamicTunnel(SshdSocketAddress address, Throwable reason) throws IOException {
        try {
            invokePortEventListenerSignaller(l -> {
                signalTornDownDynamicTunnel(l, address, reason);
                return null;
            });
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else if (t instanceof IOException) {
                throw (IOException) t;
            } else {
                throw new IOException("Failed (" + t.getClass().getSimpleName() + ")"
                        + " to signal torn down dynamic tunnel for address=" + address, t);
            }
        }
    }

    protected void signalTornDownDynamicTunnel(
            PortForwardingEventListener listener, SshdSocketAddress address, Throwable reason)
            throws IOException {
        if (listener == null) {
            return;
        }

        listener.tornDownDynamicTunnel(getSession(), address, reason);
    }

    @Override
    public synchronized SshdSocketAddress getForwardedPort(int remotePort) {
        synchronized (remoteToLocal) {
            return remoteToLocal.get(remotePort);
        }
    }

    @Override
    public synchronized SshdSocketAddress localPortForwardingRequested(SshdSocketAddress local) throws IOException {
        Objects.requireNonNull(local, "Local address is null");
        ValidateUtils.checkTrue(local.getPort() >= 0, "Invalid local port: %s", local);

        Session session = getSession();
        FactoryManager manager = Objects.requireNonNull(session.getFactoryManager(), "No factory manager");
        ForwardingFilter filter = manager.getTcpipForwardingFilter();
        try {
            if ((filter == null) || (!filter.canListen(local, session))) {
                return null;
            }
        } catch (Error e) {
            throw new RuntimeSshException(e);
        }

        signalEstablishingExplicitTunnel(local, null, true);
        SshdSocketAddress result;
        try {
            InetSocketAddress bound = doBind(local, staticIoHandlerFactory);
            result = new SshdSocketAddress(bound.getHostString(), bound.getPort());

            boolean added;
            synchronized (localForwards) {
                // NOTE !!! it is crucial to use the bound address host name first
                added = localForwards.add(new LocalForwardingEntry(result.getHostName(), local.getHostName(), result.getPort()));
            }

            if (!added) {
                throw new IOException("Failed to add local port forwarding entry for " + local + " -> " + result);
            }
        } catch (IOException | RuntimeException e) {
            try {
                localPortForwardingCancelled(local);
            } catch (IOException | RuntimeException err) {
                e.addSuppressed(e);
            }
            signalEstablishedExplicitTunnel(local, null, true, null, e);
            throw e;
        }

        try {
            signalEstablishedExplicitTunnel(local, null, true, result, null);
            return result;
        } catch (IOException | RuntimeException e) {
            throw e;
        }
    }

    @Override
    public synchronized void localPortForwardingCancelled(SshdSocketAddress local) throws IOException {
        LocalForwardingEntry entry;
        synchronized (localForwards) {
            entry = LocalForwardingEntry.findMatchingEntry(local.getHostName(), local.getPort(), localForwards);
            if (entry != null) {
                localForwards.remove(entry);
            }
        }

        if ((entry != null) && (acceptor != null)) {
            signalTearingDownExplicitTunnel(entry, true);
            try {
                acceptor.unbind(entry.toInetSocketAddress());
            } catch (RuntimeException e) {
                signalTornDownExplicitTunnel(entry, true, e);
                throw e;
            }

            signalTornDownExplicitTunnel(entry, true, null);
        } else {
        }
    }

    protected void signalEstablishingExplicitTunnel(
            SshdSocketAddress local, SshdSocketAddress remote, boolean localForwarding)
            throws IOException {
        try {
            invokePortEventListenerSignaller(l -> {
                signalEstablishingExplicitTunnel(l, local, remote, localForwarding);
                return null;
            });
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else if (t instanceof IOException) {
                throw (IOException) t;
            } else {
                throw new IOException("Failed (" + t.getClass().getSimpleName() + ")"
                        + " to signal establishing explicit tunnel for local=" + local
                        + ", remote=" + remote + ", localForwarding=" + localForwarding, t);
            }
        }
    }

    protected void signalEstablishingExplicitTunnel(PortForwardingEventListener listener,
                                                    SshdSocketAddress local, SshdSocketAddress remote, boolean localForwarding)
            throws IOException {
        if (listener == null) {
            return;
        }

        listener.establishingExplicitTunnel(getSession(), local, remote, localForwarding);
    }

    protected void signalEstablishedExplicitTunnel(
            SshdSocketAddress local, SshdSocketAddress remote, boolean localForwarding,
            SshdSocketAddress boundAddress, Throwable reason)
            throws IOException {
        try {
            invokePortEventListenerSignaller(l -> {
                signalEstablishedExplicitTunnel(l, local, remote, localForwarding, boundAddress, reason);
                return null;
            });
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else if (t instanceof IOException) {
                throw (IOException) t;
            } else {
                throw new IOException("Failed (" + t.getClass().getSimpleName() + ")"
                        + " to signal established explicit tunnel for local=" + local
                        + ", remote=" + remote + ", localForwarding=" + localForwarding
                        + ", bound=" + boundAddress, t);
            }
        }
    }

    protected void signalEstablishedExplicitTunnel(PortForwardingEventListener listener,
                                                   SshdSocketAddress local, SshdSocketAddress remote, boolean localForwarding,
                                                   SshdSocketAddress boundAddress, Throwable reason)
            throws IOException {
        if (listener == null) {
            return;
        }

        listener.establishedExplicitTunnel(getSession(), local, remote, localForwarding, boundAddress, reason);
    }

    protected void invokePortEventListenerSignaller(Invoker<PortForwardingEventListener, Void> invoker) throws Throwable {
        Throwable err = null;
        try {
            invokePortEventListenerSignallerListeners(getDefaultListeners(), invoker);
        } catch (Throwable t) {
            Throwable e = GenericUtils.peelException(t);
            err = GenericUtils.accumulateException(err, e);
        }

        try {
            invokePortEventListenerSignallerHolders(managersHolder, invoker);
        } catch (Throwable t) {
            Throwable e = GenericUtils.peelException(t);
            err = GenericUtils.accumulateException(err, e);
        }


        if (err != null) {
            throw err;
        }
    }

    protected void invokePortEventListenerSignallerListeners(
            Collection<? extends PortForwardingEventListener> listeners, Invoker<PortForwardingEventListener, Void> invoker)
            throws Throwable {
        if (GenericUtils.isEmpty(listeners)) {
            return;
        }

        Throwable err = null;
        // Need to go over the hierarchy (session, factory managed, connection service, etc...)
        for (PortForwardingEventListener l : listeners) {
            if (l == null) {
                continue;
            }

            try {
                invoker.invoke(l);
            } catch (Throwable t) {
                Throwable e = GenericUtils.peelException(t);
                err = GenericUtils.accumulateException(err, e);
            }
        }

        if (err != null) {
            throw err;
        }
    }

    protected void invokePortEventListenerSignallerHolders(
            Collection<? extends PortForwardingEventListenerManager> holders, Invoker<PortForwardingEventListener, Void> invoker)
            throws Throwable {
        if (GenericUtils.isEmpty(holders)) {
            return;
        }

        Throwable err = null;
        // Need to go over the hierarchy (session, factory managed, connection service, etc...)
        for (PortForwardingEventListenerManager m : holders) {
            try {
                PortForwardingEventListener listener = m.getPortForwardingEventListenerProxy();
                if (listener != null) {
                    invoker.invoke(listener);
                }
            } catch (Throwable t) {
                Throwable e = GenericUtils.peelException(t);
                err = GenericUtils.accumulateException(err, e);
            }

            if (m instanceof PortForwardingEventListenerManagerHolder) {
                try {
                    invokePortEventListenerSignallerHolders(((PortForwardingEventListenerManagerHolder) m).getRegisteredManagers(), invoker);
                } catch (Throwable t) {
                    Throwable e = GenericUtils.peelException(t);
                    err = GenericUtils.accumulateException(err, e);
                }
            }
        }

        if (err != null) {
            throw err;
        }
    }

    @Override
    protected synchronized Closeable getInnerCloseable() {
        return builder().parallel(toString(), dynamicLocal.values()).close(acceptor).build();
    }

    @Override
    protected void preClose() {
        this.listeners.clear();
        this.managersHolder.clear();
        super.preClose();
    }

    /**
     * @param address        The request bind address
     * @param handlerFactory A {@link Factory} to create an {@link IoHandler} if necessary
     * @return The {@link InetSocketAddress} to which the binding occurred
     * @throws IOException If failed to bind
     */
    private InetSocketAddress doBind(SshdSocketAddress address, Factory<? extends IoHandler> handlerFactory) throws IOException {
        if (acceptor == null) {
            Session session = getSession();
            FactoryManager manager = Objects.requireNonNull(session.getFactoryManager(), "No factory manager");
            IoServiceFactory factory = Objects.requireNonNull(manager.getIoServiceFactory(), "No I/O service factory");
            IoHandler handler = handlerFactory.create();
            acceptor = factory.createAcceptor(handler);
        }

        // TODO find a better way to determine the resulting bind address - what if multi-threaded calls...
        Set<SocketAddress> before = acceptor.getBoundAddresses();
        try {
            InetSocketAddress bindAddress = address.toInetSocketAddress();
            acceptor.bind(bindAddress);

            Set<SocketAddress> after = acceptor.getBoundAddresses();
            if (GenericUtils.size(after) > 0) {
                after.removeAll(before);
            }
            if (GenericUtils.isEmpty(after)) {
                throw new IOException("Error binding to " + address + "[" + bindAddress + "]: no local addresses bound");
            }

            if (after.size() > 1) {
                throw new IOException("Multiple local addresses have been bound for " + address + "[" + bindAddress + "]");
            }
            return (InetSocketAddress) after.iterator().next();
        } catch (IOException bindErr) {
            Set<SocketAddress> after = acceptor.getBoundAddresses();
            if (GenericUtils.isEmpty(after)) {
                close();
            }
            throw bindErr;
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getSession() + "]";
    }

    class StaticIoHandler implements IoHandler {
        StaticIoHandler() {
            super();
        }

        @Override
        @SuppressWarnings("synthetic-access")
        public void sessionCreated(final IoSession session) throws Exception {
            InetSocketAddress local = (InetSocketAddress) session.getLocalAddress();
            int localPort = local.getPort();
            SshdSocketAddress remote = localToRemote.get(localPort);

            final TcpipClientChannel channel;
            if (remote != null) {
                channel = new TcpipClientChannel(TcpipClientChannel.Type.Direct, session, remote);
            } else {
                channel = new TcpipClientChannel(TcpipClientChannel.Type.Forwarded, session, null);
            }
            session.setAttribute(TcpipClientChannel.class, channel);

            service.registerChannel(channel);
            channel.open().addListener(future -> {
                Throwable t = future.getException();
                if (t != null) {
                    DefaultTcpipForwarder.this.service.unregisterChannel(channel);
                    channel.close(false);
                }
            });
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {
            TcpipClientChannel channel = (TcpipClientChannel) session.getAttribute(TcpipClientChannel.class);
            if (channel != null) {
                channel.close(false);
            }
        }

        @Override
        public void messageReceived(IoSession session, Readable message) throws Exception {
            TcpipClientChannel channel = (TcpipClientChannel) session.getAttribute(TcpipClientChannel.class);
            Buffer buffer = new ByteArrayBuffer(message.available() + Long.SIZE, false);
            buffer.putBuffer(message);

            Collection<ClientChannelEvent> result = channel.waitFor(STATIC_IO_MSG_RECEIVED_EVENTS, Long.MAX_VALUE);

            OutputStream outputStream = channel.getInvertedIn();
            outputStream.write(buffer.array(), buffer.rpos(), buffer.available());
            outputStream.flush();
        }

        @Override
        @SuppressWarnings("synthetic-access")
        public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
            session.close(false);
        }
    }
}

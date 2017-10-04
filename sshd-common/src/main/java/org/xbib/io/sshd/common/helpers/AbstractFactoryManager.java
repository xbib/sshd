package org.xbib.io.sshd.common.helpers;

import org.xbib.io.sshd.common.agent.SshAgentFactory;
import org.xbib.io.sshd.common.AttributeStore;
import org.xbib.io.sshd.common.Factory;
import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.PropertyResolver;
import org.xbib.io.sshd.common.PropertyResolverUtils;
import org.xbib.io.sshd.common.ServiceFactory;
import org.xbib.io.sshd.common.SyspropsMapWrapper;
import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.channel.ChannelListener;
import org.xbib.io.sshd.common.channel.RequestHandler;
import org.xbib.io.sshd.common.config.VersionProperties;
import org.xbib.io.sshd.common.file.FileSystemFactory;
import org.xbib.io.sshd.common.forward.ForwardingFilter;
import org.xbib.io.sshd.common.forward.PortForwardingEventListener;
import org.xbib.io.sshd.common.forward.TcpipForwarderFactory;
import org.xbib.io.sshd.common.io.DefaultIoServiceFactoryFactory;
import org.xbib.io.sshd.common.io.IoServiceFactory;
import org.xbib.io.sshd.common.io.IoServiceFactoryFactory;
import org.xbib.io.sshd.common.kex.AbstractKexFactoryManager;
import org.xbib.io.sshd.common.random.Random;
import org.xbib.io.sshd.common.session.ConnectionService;
import org.xbib.io.sshd.common.session.ReservedSessionMessagesHandler;
import org.xbib.io.sshd.common.session.SessionListener;
import org.xbib.io.sshd.common.session.helpers.AbstractSessionFactory;
import org.xbib.io.sshd.common.session.helpers.SessionTimeoutListener;
import org.xbib.io.sshd.common.util.EventListenerUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.threads.ThreadUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public abstract class AbstractFactoryManager extends AbstractKexFactoryManager implements FactoryManager {
    protected final Collection<SessionListener> sessionListeners = new CopyOnWriteArraySet<>();
    protected final SessionListener sessionListenerProxy;
    protected final Collection<ChannelListener> channelListeners = new CopyOnWriteArraySet<>();
    protected final ChannelListener channelListenerProxy;
    protected final Collection<PortForwardingEventListener> tunnelListeners = new CopyOnWriteArraySet<>();
    protected final PortForwardingEventListener tunnelListenerProxy;
    private final Map<String, Object> properties = new ConcurrentHashMap<>();
    private final Map<AttributeKey<?>, Object> attributes = new ConcurrentHashMap<>();
    protected IoServiceFactoryFactory ioServiceFactoryFactory;
    protected IoServiceFactory ioServiceFactory;
    protected Factory<Random> randomFactory;
    protected List<NamedFactory<Channel>> channelFactories;
    protected SshAgentFactory agentFactory;
    protected ScheduledExecutorService executor;
    protected boolean shutdownExecutor;
    protected TcpipForwarderFactory tcpipForwarderFactory;
    protected ForwardingFilter tcpipForwardingFilter;
    protected FileSystemFactory fileSystemFactory;
    protected List<ServiceFactory> serviceFactories;
    protected List<RequestHandler<ConnectionService>> globalRequestHandlers;
    protected SessionTimeoutListener sessionTimeoutListener;
    protected ScheduledFuture<?> timeoutListenerFuture;
    private PropertyResolver parentResolver = SyspropsMapWrapper.SYSPROPS_RESOLVER;
    private ReservedSessionMessagesHandler reservedSessionMessagesHandler;

    protected AbstractFactoryManager() {
        ClassLoader loader = getClass().getClassLoader();
        sessionListenerProxy = EventListenerUtils.proxyWrapper(SessionListener.class, loader, sessionListeners);
        channelListenerProxy = EventListenerUtils.proxyWrapper(ChannelListener.class, loader, channelListeners);
        tunnelListenerProxy = EventListenerUtils.proxyWrapper(PortForwardingEventListener.class, loader, tunnelListeners);
    }

    @Override
    public IoServiceFactory getIoServiceFactory() {
        synchronized (ioServiceFactoryFactory) {
            if (ioServiceFactory == null) {
                ioServiceFactory = ioServiceFactoryFactory.create(this);
            }
        }
        return ioServiceFactory;
    }

    public IoServiceFactoryFactory getIoServiceFactoryFactory() {
        return ioServiceFactoryFactory;
    }

    public void setIoServiceFactoryFactory(IoServiceFactoryFactory ioServiceFactory) {
        this.ioServiceFactoryFactory = ioServiceFactory;
    }

    @Override
    public Factory<Random> getRandomFactory() {
        return randomFactory;
    }

    public void setRandomFactory(Factory<Random> randomFactory) {
        this.randomFactory = randomFactory;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(AttributeKey<T> key) {
        return (T) attributes.get(Objects.requireNonNull(key, "No key"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T setAttribute(AttributeKey<T> key, T value) {
        return (T) attributes.put(
                Objects.requireNonNull(key, "No key"),
                Objects.requireNonNull(value, "No value"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T removeAttribute(AttributeKey<T> key) {
        return (T) attributes.remove(Objects.requireNonNull(key, "No key"));
    }

    @Override
    public <T> T resolveAttribute(AttributeKey<T> key) {
        return AttributeStore.resolveAttribute(this, key);
    }

    @Override
    public PropertyResolver getParentPropertyResolver() {
        return parentResolver;
    }

    public void setParentPropertyResolver(PropertyResolver parent) {
        parentResolver = parent;
    }

    @Override
    public String getVersion() {
        return PropertyResolverUtils.getStringProperty(VersionProperties.getVersionProperties(), "sshd-version", DEFAULT_VERSION).toUpperCase();
    }

    @Override
    public List<NamedFactory<Channel>> getChannelFactories() {
        return channelFactories;
    }

    public void setChannelFactories(List<NamedFactory<Channel>> channelFactories) {
        this.channelFactories = channelFactories;
    }

    public int getNioWorkers() {
        int nb = this.getIntProperty(NIO_WORKERS, DEFAULT_NIO_WORKERS);
        if (nb > 0) {
            return nb;
        } else {    // it may have been configured to a negative value
            return DEFAULT_NIO_WORKERS;
        }
    }

    public void setNioWorkers(int nioWorkers) {
        if (nioWorkers > 0) {
            PropertyResolverUtils.updateProperty(this, NIO_WORKERS, nioWorkers);
        } else {
            PropertyResolverUtils.updateProperty(this, NIO_WORKERS, null);
        }
    }

    @Override
    public SshAgentFactory getAgentFactory() {
        return agentFactory;
    }

    public void setAgentFactory(SshAgentFactory agentFactory) {
        this.agentFactory = agentFactory;
    }

    @Override
    public ScheduledExecutorService getScheduledExecutorService() {
        return executor;
    }

    public void setScheduledExecutorService(ScheduledExecutorService executor) {
        setScheduledExecutorService(executor, false);
    }

    public void setScheduledExecutorService(ScheduledExecutorService executor, boolean shutdownExecutor) {
        this.executor = executor;
        this.shutdownExecutor = shutdownExecutor;
    }

    @Override
    public TcpipForwarderFactory getTcpipForwarderFactory() {
        return tcpipForwarderFactory;
    }

    public void setTcpipForwarderFactory(TcpipForwarderFactory tcpipForwarderFactory) {
        this.tcpipForwarderFactory = tcpipForwarderFactory;
    }

    @Override
    public ForwardingFilter getTcpipForwardingFilter() {
        return tcpipForwardingFilter;
    }

    public void setTcpipForwardingFilter(ForwardingFilter tcpipForwardingFilter) {
        this.tcpipForwardingFilter = tcpipForwardingFilter;
    }

    @Override
    public FileSystemFactory getFileSystemFactory() {
        return fileSystemFactory;
    }

    public void setFileSystemFactory(FileSystemFactory fileSystemFactory) {
        this.fileSystemFactory = fileSystemFactory;
    }

    @Override
    public List<ServiceFactory> getServiceFactories() {
        return serviceFactories;
    }

    public void setServiceFactories(List<ServiceFactory> serviceFactories) {
        this.serviceFactories = serviceFactories;
    }

    @Override
    public List<RequestHandler<ConnectionService>> getGlobalRequestHandlers() {
        return globalRequestHandlers;
    }

    public void setGlobalRequestHandlers(List<RequestHandler<ConnectionService>> globalRequestHandlers) {
        this.globalRequestHandlers = globalRequestHandlers;
    }

    @Override
    public ReservedSessionMessagesHandler getReservedSessionMessagesHandler() {
        return reservedSessionMessagesHandler;
    }

    @Override
    public void setReservedSessionMessagesHandler(ReservedSessionMessagesHandler handler) {
        reservedSessionMessagesHandler = handler;
    }

    @Override
    public void addSessionListener(SessionListener listener) {
        SessionListener.validateListener(listener);

        // avoid race conditions on notifications while manager is being closed
        if (!isOpen()) {
            return;
        }

        if (this.sessionListeners.add(listener)) {
        } else {
        }
    }

    @Override
    public void removeSessionListener(SessionListener listener) {
        if (listener == null) {
            return;
        }

        SessionListener.validateListener(listener);

        if (this.sessionListeners.remove(listener)) {
        } else {
        }
    }

    @Override
    public SessionListener getSessionListenerProxy() {
        return sessionListenerProxy;
    }

    @Override
    public void addChannelListener(ChannelListener listener) {
        ChannelListener.validateListener(listener);

        // avoid race conditions on notifications while manager is being closed
        if (!isOpen()) {
            return;
        }

        if (this.channelListeners.add(listener)) {
        } else {
        }
    }

    @Override
    public void removeChannelListener(ChannelListener listener) {
        if (listener == null) {
            return;
        }

        ChannelListener.validateListener(listener);
        if (this.channelListeners.remove(listener)) {
        } else {
        }
    }

    @Override
    public ChannelListener getChannelListenerProxy() {
        return channelListenerProxy;
    }

    @Override
    public PortForwardingEventListener getPortForwardingEventListenerProxy() {
        return tunnelListenerProxy;
    }

    @Override
    public void addPortForwardingEventListener(PortForwardingEventListener listener) {
        PortForwardingEventListener.validateListener(listener);

        // avoid race conditions on notifications while session is being closed
        if (!isOpen()) {
            return;
        }

        if (this.tunnelListeners.add(listener)) {
        } else {
        }
    }

    @Override
    public void removePortForwardingEventListener(PortForwardingEventListener listener) {
        if (listener == null) {
            return;
        }

        PortForwardingEventListener.validateListener(listener);
        if (this.tunnelListeners.remove(listener)) {
        } else {
        }
    }

    protected void setupSessionTimeout(AbstractSessionFactory<?, ?> sessionFactory) {
        // set up the the session timeout listener and schedule it
        sessionTimeoutListener = createSessionTimeoutListener();
        addSessionListener(sessionTimeoutListener);

        timeoutListenerFuture = getScheduledExecutorService()
                .scheduleAtFixedRate(sessionTimeoutListener, 1, 1, TimeUnit.SECONDS);
    }

    protected void removeSessionTimeout(AbstractSessionFactory<?, ?> sessionFactory) {
        stopSessionTimeoutListener(sessionFactory);
    }

    protected SessionTimeoutListener createSessionTimeoutListener() {
        return new SessionTimeoutListener();
    }

    protected void stopSessionTimeoutListener(AbstractSessionFactory<?, ?> sessionFactory) {
        // cancel the timeout monitoring task
        if (timeoutListenerFuture != null) {
            try {
                timeoutListenerFuture.cancel(true);
            } finally {
                timeoutListenerFuture = null;
            }
        }

        // remove the sessionTimeoutListener completely; should the SSH server/client be restarted, a new one
        // will be created.
        if (sessionTimeoutListener != null) {
            try {
                removeSessionListener(sessionTimeoutListener);
            } finally {
                sessionTimeoutListener = null;
            }
        }
    }

    protected void checkConfig() {
        ValidateUtils.checkNotNullAndNotEmpty(getKeyExchangeFactories(), "KeyExchangeFactories not set");

        if (getScheduledExecutorService() == null) {
            setScheduledExecutorService(
                    ThreadUtils.newSingleThreadScheduledExecutor(this.toString() + "-timer"),
                    true);
        }

        ValidateUtils.checkNotNullAndNotEmpty(getCipherFactories(), "CipherFactories not set");
        ValidateUtils.checkNotNullAndNotEmpty(getCompressionFactories(), "CompressionFactories not set");
        ValidateUtils.checkNotNullAndNotEmpty(getMacFactories(), "MacFactories not set");

        Objects.requireNonNull(getRandomFactory(), "RandomFactory not set");

        if (getIoServiceFactoryFactory() == null) {
            setIoServiceFactoryFactory(new DefaultIoServiceFactoryFactory());
        }
    }
}

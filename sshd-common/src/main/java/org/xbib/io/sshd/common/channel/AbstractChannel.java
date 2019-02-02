package org.xbib.io.sshd.common.channel;

import org.xbib.io.sshd.common.AttributeStore;
import org.xbib.io.sshd.common.Closeable;
import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.PropertyResolver;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.future.CloseFuture;
import org.xbib.io.sshd.common.future.DefaultCloseFuture;
import org.xbib.io.sshd.common.future.SshFutureListener;
import org.xbib.io.sshd.common.io.AbstractIoWriteFuture;
import org.xbib.io.sshd.common.io.IoWriteFuture;
import org.xbib.io.sshd.common.session.ConnectionService;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.EventListenerUtils;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.Int2IntFunction;
import org.xbib.io.sshd.common.util.Invoker;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.BufferUtils;
import org.xbib.io.sshd.common.util.closeable.AbstractInnerCloseable;
import org.xbib.io.sshd.common.util.closeable.IoBaseCloseable;
import org.xbib.io.sshd.common.util.io.IoUtils;
import org.xbib.io.sshd.common.util.threads.ExecutorServiceConfigurer;

import java.io.EOFException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntUnaryOperator;

/**
 * Provides common client/server channel functionality.
 */
public abstract class AbstractChannel
        extends AbstractInnerCloseable
        implements Channel, ExecutorServiceConfigurer {

    /**
     * Default growth factor function used to resize response buffers
     */
    public static final IntUnaryOperator RESPONSE_BUFFER_GROWTH_FACTOR = Int2IntFunction.add(Byte.SIZE);
    protected final AtomicBoolean initialized = new AtomicBoolean(false);
    protected final AtomicBoolean eofReceived = new AtomicBoolean(false);
    protected final AtomicBoolean eofSent = new AtomicBoolean(false);
    protected final DefaultCloseFuture gracefulFuture;
    /**
     * Channel events listener
     */
    protected final Collection<ChannelListener> channelListeners = new CopyOnWriteArraySet<>();
    protected final ChannelListener channelListenerProxy;
    private final List<RequestHandler<Channel>> requestHandlers = new CopyOnWriteArrayList<>();
    private final Window localWindow;
    private final Window remoteWindow;
    /**
     * A {@link Map} of sent requests - key = request name, value = timestamp when
     * request was sent.
     */
    private final Map<String, Date> pendingRequests = new ConcurrentHashMap<>();
    private final Map<String, Object> properties = new ConcurrentHashMap<>();
    private final Map<AttributeKey<?>, Object> attributes = new ConcurrentHashMap<>();
    protected ConnectionService service;
    protected AtomicReference<GracefulState> gracefulState = new AtomicReference<>(GracefulState.Opened);
    private int id = -1;
    private int recipient = -1;
    private Session sessionInstance;
    private ExecutorService executor;
    private boolean shutdownExecutor;
    protected AbstractChannel(boolean client) {
        this("", client);
    }

    protected AbstractChannel(boolean client, Collection<? extends RequestHandler<Channel>> handlers) {
        this("", client, handlers);
    }

    protected AbstractChannel(String discriminator, boolean client) {
        this(discriminator, client, Collections.emptyList());
    }

    protected AbstractChannel(String discriminator, boolean client, Collection<? extends RequestHandler<Channel>> handlers) {
        super(discriminator);
        gracefulFuture = new DefaultCloseFuture(discriminator, lock);
        localWindow = new Window(this, null, client, true);
        remoteWindow = new Window(this, null, client, false);
        channelListenerProxy = EventListenerUtils.proxyWrapper(ChannelListener.class, getClass().getClassLoader(), channelListeners);
        addRequestHandlers(handlers);
    }

    @Override
    public List<RequestHandler<Channel>> getRequestHandlers() {
        return requestHandlers;
    }

    @Override
    public void addRequestHandler(RequestHandler<Channel> handler) {
        requestHandlers.add(Objects.requireNonNull(handler, "No handler instance"));
    }

    @Override
    public void removeRequestHandler(RequestHandler<Channel> handler) {
        requestHandlers.remove(Objects.requireNonNull(handler, "No handler instance"));
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getRecipient() {
        return recipient;
    }

    protected void setRecipient(int recipient) {
        this.recipient = recipient;
    }

    @Override
    public Window getLocalWindow() {
        return localWindow;
    }

    @Override
    public Window getRemoteWindow() {
        return remoteWindow;
    }

    @Override
    public Session getSession() {
        return sessionInstance;
    }

    @Override
    public PropertyResolver getParentPropertyResolver() {
        return getSession();
    }

    @Override
    public ExecutorService getExecutorService() {
        return executor;
    }

    @Override
    public void setExecutorService(ExecutorService service) {
        executor = service;
    }

    @Override
    public boolean isShutdownOnExit() {
        return shutdownExecutor;
    }

    @Override
    public void setShutdownOnExit(boolean shutdown) {
        shutdownExecutor = shutdown;
    }

    /**
     * Add a channel request to the tracked pending ones if reply is expected
     *
     * @param request   The request type
     * @param wantReply {@code true} if reply is expected
     * @return The allocated {@link Date} timestamp - {@code null} if no reply
     * is expected (in which case the request is not tracked)
     * @throws IllegalArgumentException If the request is already being tracked
     * @see #removePendingRequest(String)
     */
    protected Date addPendingRequest(String request, boolean wantReply) {
        if (!wantReply) {
            return null;
        }

        Date pending = new Date(System.currentTimeMillis());
        Date prev = pendingRequests.put(request, pending);
        ValidateUtils.checkTrue(prev == null, "Multiple pending requests of type=%s", request);
        return pending;
    }

    /**
     * Removes a channel request from the tracked ones
     *
     * @param request The request type
     * @return The allocated {@link Date} timestamp - {@code null} if the
     * specified request type is not being tracked or has not been added to
     * the tracked ones to begin with
     * @see #addPendingRequest(String, boolean)
     */
    protected Date removePendingRequest(String request) {
        Date pending = pendingRequests.remove(request);
        return pending;
    }

    @Override
    public void handleRequest(Buffer buffer) throws IOException {
        handleChannelRequest(buffer.getString(), buffer.getBoolean(), buffer);
    }

    protected void handleChannelRequest(String req, boolean wantReply, Buffer buffer) throws IOException {

        Collection<? extends RequestHandler<Channel>> handlers = getRequestHandlers();
        for (RequestHandler<Channel> handler : handlers) {
            RequestHandler.Result result;
            try {
                result = handler.process(this, req, wantReply, buffer);
            } catch (Throwable e) {
                result = RequestHandler.Result.ReplyFailure;
            }

            // if Unsupported then check the next handler in line
            if (RequestHandler.Result.Unsupported.equals(result)) {
            } else {
                sendResponse(buffer, req, result, wantReply);
                return;
            }
        }

        // none of the handlers processed the request
        handleUnknownChannelRequest(req, wantReply, buffer);
    }

    /**
     * Called when none of the register request handlers reported handling the request
     *
     * @param req       The request type
     * @param wantReply Whether reply is requested
     * @param buffer    The {@link Buffer} containing extra request-specific data
     * @throws IOException If failed to send the response (if needed)
     * @see #handleInternalRequest(String, boolean, Buffer)
     */
    protected void handleUnknownChannelRequest(String req, boolean wantReply, Buffer buffer) throws IOException {
        RequestHandler.Result r = handleInternalRequest(req, wantReply, buffer);
        if ((r == null) || RequestHandler.Result.Unsupported.equals(r)) {
            sendResponse(buffer, req, RequestHandler.Result.Unsupported, wantReply);
        } else {
            sendResponse(buffer, req, r, wantReply);
        }
    }

    /**
     * Called by {@link #handleUnknownChannelRequest(String, boolean, Buffer)}
     * in order to allow channel request handling if none of the registered handlers
     * processed the request - last chance.
     *
     * @param req       The request type
     * @param wantReply Whether reply is requested
     * @param buffer    The {@link Buffer} containing extra request-specific data
     * @return The handling result - if {@code null} or {@code Unsupported}
     * and reply is required then a failure message will be sent
     * @throws IOException If failed to process the request internally
     */
    protected RequestHandler.Result handleInternalRequest(String req, boolean wantReply, Buffer buffer) throws IOException {
        return RequestHandler.Result.Unsupported;
    }

    protected IoWriteFuture sendResponse(Buffer buffer, String req, RequestHandler.Result result, boolean wantReply) throws IOException {
        if (RequestHandler.Result.Replied.equals(result) || (!wantReply)) {
            return new AbstractIoWriteFuture(req, null) {
                {
                    setValue(Boolean.TRUE);
                }
            };
        }

        byte cmd = RequestHandler.Result.ReplySuccess.equals(result)
                ? SshConstants.SSH_MSG_CHANNEL_SUCCESS
                : SshConstants.SSH_MSG_CHANNEL_FAILURE;
        Session session = getSession();
        Buffer rsp = session.createBuffer(cmd, Integer.BYTES);
        rsp.putInt(recipient);
        return session.writePacket(rsp);
    }

    @Override
    public void init(ConnectionService service, Session session, int id) throws IOException {
        this.service = service;
        this.sessionInstance = session;
        this.id = id;

        signalChannelInitialized();
        configureWindow();
        initialized.set(true);
    }

    protected void signalChannelInitialized() throws IOException {
        try {
            invokeChannelSignaller(l -> {
                signalChannelInitialized(l);
                return null;
            });
        } catch (Throwable err) {
            Throwable e = GenericUtils.peelException(err);
            if (e instanceof IOException) {
                throw (IOException) e;
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new IOException("Failed (" + e.getClass().getSimpleName() + ") to notify channel " + this + " initialization: " + e.getMessage(), e);
            }
        }
    }

    protected void signalChannelInitialized(ChannelListener listener) {
        if (listener == null) {
            return;
        }

        listener.channelInitialized(this);
    }

    protected void signalChannelOpenSuccess() {
        try {
            invokeChannelSignaller(l -> {
                signalChannelOpenSuccess(l);
                return null;
            });
        } catch (Throwable err) {
            if (err instanceof RuntimeException) {
                throw (RuntimeException) err;
            } else if (err instanceof Error) {
                throw (Error) err;
            } else {
                throw new RuntimeException(err);
            }
        }
    }

    protected void signalChannelOpenSuccess(ChannelListener listener) {
        if (listener == null) {
            return;
        }

        listener.channelOpenSuccess(this);
    }

    @Override
    public boolean isInitialized() {
        return initialized.get();
    }

    protected void signalChannelOpenFailure(Throwable reason) {
        try {
            invokeChannelSignaller(l -> {
                signalChannelOpenFailure(l, reason);
                return null;
            });
        } catch (Throwable err) {
            Throwable ignored = GenericUtils.peelException(err);
        }
    }

    protected void signalChannelOpenFailure(ChannelListener listener, Throwable reason) {
        if (listener == null) {
            return;
        }

        listener.channelOpenFailure(this, reason);
    }

    protected void notifyStateChanged(String hint) {
        try {
            invokeChannelSignaller(l -> {
                notifyStateChanged(l, hint);
                return null;
            });
        } catch (Throwable err) {
            Throwable e = GenericUtils.peelException(err);
        } finally {
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    protected void notifyStateChanged(ChannelListener listener, String hint) {
        if (listener == null) {
            return;
        }

        listener.channelStateChanged(this, hint);
    }

    @Override
    public void addChannelListener(ChannelListener listener) {
        ChannelListener.validateListener(listener);
        // avoid race conditions on notifications while channel is being closed
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
    public void handleClose() throws IOException {

        if (!eofSent.getAndSet(true)) {
        }

        if (gracefulState.compareAndSet(GracefulState.Opened, GracefulState.CloseReceived)) {
            close(false);
        } else if (gracefulState.compareAndSet(GracefulState.CloseSent, GracefulState.Closed)) {
            gracefulFuture.setClosed();
        }
    }

    @Override
    public CloseFuture close(boolean immediately) {
        if (!eofSent.getAndSet(true)) {
        }

        return super.close(immediately);
    }

    @Override
    protected Closeable getInnerCloseable() {
        return new GracefulChannelCloseable();
    }

    @Override
    protected void preClose() {
        try {
            signalChannelClosed(null);
        } finally {
            // clear the listeners since we are closing the channel (quicker GC)
            this.channelListeners.clear();
        }

        IOException err = IoUtils.closeQuietly(getLocalWindow(), getRemoteWindow());
        if (err != null) {
        }

        super.preClose();
    }

    public void signalChannelClosed(Throwable reason) {
        try {
            invokeChannelSignaller(l -> {
                signalChannelClosed(l, reason);
                return null;
            });
        } catch (Throwable err) {
            Throwable e = GenericUtils.peelException(err);
        }
    }

    protected void signalChannelClosed(ChannelListener listener, Throwable reason) {
        if (listener == null) {
            return;
        }

        listener.channelClosed(this, reason);
    }

    protected void invokeChannelSignaller(Invoker<ChannelListener, Void> invoker) throws Throwable {
        Session session = getSession();
        FactoryManager manager = (session == null) ? null : session.getFactoryManager();
        ChannelListener[] listeners = {
                (manager == null) ? null : manager.getChannelListenerProxy(),
                (session == null) ? null : session.getChannelListenerProxy(),
                getChannelListenerProxy()
        };

        Throwable err = null;
        for (ChannelListener l : listeners) {
            if (l == null) {
                continue;
            }
            try {
                invoker.invoke(l);
            } catch (Throwable t) {
                err = GenericUtils.accumulateException(err, t);
            }
        }

        if (err != null) {
            throw err;
        }
    }

    @Override
    protected void doCloseImmediately() {
        if (service != null) {
            service.unregisterChannel(AbstractChannel.this);
        }

        super.doCloseImmediately();
    }

    protected IoWriteFuture writePacket(Buffer buffer) throws IOException {
        Session s = getSession();
        if (!isClosing()) {
            return s.writePacket(buffer);
        } else {
            return new AbstractIoWriteFuture(s.toString(),null) {
                {
                    setValue(new EOFException("Channel is being closed"));
                }
            };
        }
    }

    @Override
    public void handleData(Buffer buffer) throws IOException {
        long len = validateIncomingDataSize(SshConstants.SSH_MSG_CHANNEL_DATA, buffer.getUInt());
        if (isEofSignalled()) {
            // TODO consider throwing an exception
        }
        doWriteData(buffer.array(), buffer.rpos(), len);
    }

    @Override
    public void handleExtendedData(Buffer buffer) throws IOException {
        int ex = buffer.getInt();
        // Only accept extended data for stderr
        if (ex != SshConstants.SSH_EXTENDED_DATA_STDERR) {
            Session s = getSession();
            Buffer rsp = s.createBuffer(SshConstants.SSH_MSG_CHANNEL_FAILURE, Integer.BYTES);
            rsp.putInt(getRecipient());
            writePacket(rsp);
            return;
        }

        long len = validateIncomingDataSize(SshConstants.SSH_MSG_CHANNEL_EXTENDED_DATA, buffer.getUInt());
        if (isEofSignalled()) {
            // TODO consider throwing an exception
        }
        doWriteExtendedData(buffer.array(), buffer.rpos(), len);
    }

    protected long validateIncomingDataSize(int cmd, long len /* actually a uint32 */) {
        if (!BufferUtils.isValidUint32Value(len)) {
            throw new IllegalArgumentException("Non UINT32 length (" + len + ") for command=" + SshConstants.getCommandMessageName(cmd));
        }

        /*
         * According to RFC 4254 section 5.1
         *
         *      The 'maximum packet size' specifies the maximum size of an
         *      individual data packet that can be sent to the sender
         *
         * The local window reflects our preference - i.e., how much our peer
         * should send at most
         */
        Window wLocal = getLocalWindow();
        long maxLocalSize = wLocal.getPacketSize();

        /*
         * The reason for the +4 is that there seems to be some confusion whether
         * the max. packet size includes the length field or not
         */
        if (len > (maxLocalSize + 4L)) {
            throw new IllegalStateException("Bad length (" + len + ") "
                    + " for cmd=" + SshConstants.getCommandMessageName(cmd)
                    + " - max. allowed=" + maxLocalSize);
        }

        return len;
    }

    @Override
    public void handleEof() throws IOException {
        if (eofReceived.getAndSet(true)) {
            // TODO consider throwing an exception
        } else {
        }
        notifyStateChanged("SSH_MSG_CHANNEL_EOF");
    }

    @Override
    public boolean isEofSignalled() {
        return eofReceived.get();
    }

    @Override
    public void handleWindowAdjust(Buffer buffer) throws IOException {
        int window = buffer.getInt();
        Window wRemote = getRemoteWindow();
        wRemote.expand(window);
    }

    @Override
    public void handleSuccess() throws IOException {
    }

    @Override
    public void handleFailure() throws IOException {
        // TODO: do something to report failed requests?
    }

    protected abstract void doWriteData(byte[] data, int off, long len) throws IOException;

    protected abstract void doWriteExtendedData(byte[] data, int off, long len) throws IOException;

    protected void sendEof() throws IOException {
        if (eofSent.getAndSet(true)) {
            return;
        }

        if (isClosing()) {
            return;
        }

        Session s = getSession();
        Buffer buffer = s.createBuffer(SshConstants.SSH_MSG_CHANNEL_EOF, Short.SIZE);
        buffer.putInt(getRecipient());
        writePacket(buffer);
    }

    public boolean isEofSent() {
        return eofSent.get();
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

    protected void configureWindow() {
        localWindow.init(this);
    }

    protected void sendWindowAdjust(long len) throws IOException {
        Session s = getSession();
        Buffer buffer = s.createBuffer(SshConstants.SSH_MSG_CHANNEL_WINDOW_ADJUST, Short.SIZE);
        buffer.putInt(getRecipient());
        buffer.putInt(len);
        writePacket(buffer);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[id=" + getId() + ", recipient=" + getRecipient() + "]" + "-" + getSession();
    }

    protected enum GracefulState {
        Opened, CloseSent, CloseReceived, Closed
    }

    public class GracefulChannelCloseable extends IoBaseCloseable {
        private final AtomicBoolean closing = new AtomicBoolean(false);

        public GracefulChannelCloseable() {
            super();
        }

        @Override
        public void addCloseFutureListener(SshFutureListener<CloseFuture> listener) {
            gracefulFuture.addListener(listener);
        }

        @Override
        public void removeCloseFutureListener(SshFutureListener<CloseFuture> listener) {
            gracefulFuture.removeListener(listener);
        }

        @Override
        public boolean isClosing() {
            return closing.get();
        }

        public void setClosing(boolean on) {
            closing.set(on);
        }

        @Override
        public boolean isClosed() {
            return gracefulFuture.isClosed();
        }

        @Override
        public CloseFuture close(final boolean immediately) {
            final Channel channel = AbstractChannel.this;

            setClosing(true);
            if (immediately) {
                gracefulFuture.setClosed();
            } else if (!gracefulFuture.isClosed()) {

                Session s = getSession();
                Buffer buffer = s.createBuffer(SshConstants.SSH_MSG_CHANNEL_CLOSE, Short.SIZE);
                buffer.putInt(getRecipient());

                try {
                    long timeout = channel.getLongProperty(FactoryManager.CHANNEL_CLOSE_TIMEOUT, FactoryManager.DEFAULT_CHANNEL_CLOSE_TIMEOUT);
                    s.writePacket(buffer, timeout, TimeUnit.MILLISECONDS).addListener(future -> {
                        if (future.isWritten()) {
                            handleClosePacketWritten(channel, immediately);
                        } else {
                            handleClosePacketWriteFailure(channel, immediately, future.getException());
                        }
                    });
                } catch (IOException e) {
                    channel.close(true);
                }
            }

            ExecutorService service = getExecutorService();
            if ((service != null) && isShutdownOnExit() && (!service.isShutdown())) {
                Collection<?> running = service.shutdownNow();
            }

            return gracefulFuture;
        }

        protected void handleClosePacketWritten(Channel channel, boolean immediately) {

            if (gracefulState.compareAndSet(GracefulState.Opened, GracefulState.CloseSent)) {
                // Waiting for CLOSE message to come back from the remote side
                return;
            } else if (gracefulState.compareAndSet(GracefulState.CloseReceived, GracefulState.Closed)) {
                gracefulFuture.setClosed();
            }
        }

        protected void handleClosePacketWriteFailure(Channel channel, boolean immediately, Throwable t) {
            channel.close(true);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + AbstractChannel.this + "]";
        }
    }
}

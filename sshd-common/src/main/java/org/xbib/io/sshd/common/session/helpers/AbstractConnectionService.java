package org.xbib.io.sshd.common.session.helpers;

import org.xbib.io.sshd.common.agent.AgentForwardSupport;
import org.xbib.io.sshd.common.agent.DefaultAgentForwardSupport;
import org.xbib.io.sshd.common.Closeable;
import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.channel.AbstractChannel;
import org.xbib.io.sshd.common.channel.AbstractClientChannel;
import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.channel.OpenChannelException;
import org.xbib.io.sshd.common.channel.RequestHandler;
import org.xbib.io.sshd.common.channel.Window;
import org.xbib.io.sshd.common.forward.PortForwardingEventListener;
import org.xbib.io.sshd.common.forward.PortForwardingEventListenerManager;
import org.xbib.io.sshd.common.forward.TcpipForwarder;
import org.xbib.io.sshd.common.forward.TcpipForwarderFactory;
import org.xbib.io.sshd.common.io.AbstractIoWriteFuture;
import org.xbib.io.sshd.common.io.IoWriteFuture;
import org.xbib.io.sshd.common.session.ConnectionService;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.EventListenerUtils;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.Int2IntFunction;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.closeable.AbstractInnerCloseable;
import org.xbib.io.sshd.common.x11.DefaultX11ForwardSupport;
import org.xbib.io.sshd.common.x11.X11ForwardSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntUnaryOperator;

/**
 * Base implementation of ConnectionService.
 *
 * @param <S> Type of {@link AbstractSession} being used
 */
public abstract class AbstractConnectionService<S extends AbstractSession>
        extends AbstractInnerCloseable
        implements ConnectionService {
    /**
     * Property that can be used to configure max. allowed concurrent active channels
     *
     * @see #registerChannel(Channel)
     */
    public static final String MAX_CONCURRENT_CHANNELS_PROP = "max-sshd-channels";

    /**
     * Default value for {@link #MAX_CONCURRENT_CHANNELS_PROP} is none specified
     */
    public static final int DEFAULT_MAX_CHANNELS = Integer.MAX_VALUE;

    /**
     * Default growth factor function used to resize response buffers
     */
    public static final IntUnaryOperator RESPONSE_BUFFER_GROWTH_FACTOR = Int2IntFunction.add(Byte.SIZE);

    /**
     * Map of channels keyed by the identifier
     */
    protected final Map<Integer, Channel> channels = new ConcurrentHashMap<>();
    /**
     * Next channel identifier
     */
    protected final AtomicInteger nextChannelId = new AtomicInteger(0);

    private final AtomicReference<AgentForwardSupport> agentForwardHolder = new AtomicReference<>();
    private final AtomicReference<X11ForwardSupport> x11ForwardHolder = new AtomicReference<>();
    private final AtomicReference<TcpipForwarder> tcpipForwarderHolder = new AtomicReference<>();
    private final AtomicBoolean allowMoreSessions = new AtomicBoolean(true);
    private final Collection<PortForwardingEventListener> listeners = new CopyOnWriteArraySet<>();
    private final Collection<PortForwardingEventListenerManager> managersHolder = new CopyOnWriteArraySet<>();
    private final PortForwardingEventListener listenerProxy;
    private final S sessionInstance;

    protected AbstractConnectionService(S session) {
        sessionInstance = Objects.requireNonNull(session, "No session");
        listenerProxy = EventListenerUtils.proxyWrapper(PortForwardingEventListener.class, getClass().getClassLoader(), listeners);
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

    public Collection<Channel> getChannels() {
        return channels.values();
    }

    @Override
    public S getSession() {
        return sessionInstance;
    }

    @Override
    public void start() {
        // do nothing
    }

    @Override
    public TcpipForwarder getTcpipForwarder() {
        TcpipForwarder forwarder;
        S session = getSession();
        synchronized (tcpipForwarderHolder) {
            forwarder = tcpipForwarderHolder.get();
            if (forwarder != null) {
                return forwarder;
            }

            forwarder = ValidateUtils.checkNotNull(createTcpipForwarder(session), "No forwarder created for %s", session);
            tcpipForwarderHolder.set(forwarder);
        }

        return forwarder;
    }

    @Override
    protected void preClose() {
        this.listeners.clear();
        this.managersHolder.clear();
        super.preClose();
    }

    protected TcpipForwarder createTcpipForwarder(S session) {
        FactoryManager manager =
                Objects.requireNonNull(session.getFactoryManager(), "No factory manager");
        TcpipForwarderFactory factory =
                Objects.requireNonNull(manager.getTcpipForwarderFactory(), "No forwarder factory");
        TcpipForwarder forwarder = factory.create(this);
        forwarder.addPortForwardingEventListenerManager(this);
        return forwarder;
    }

    @Override
    public X11ForwardSupport getX11ForwardSupport() {
        X11ForwardSupport x11Support;
        S session = getSession();
        synchronized (x11ForwardHolder) {
            x11Support = x11ForwardHolder.get();
            if (x11Support != null) {
                return x11Support;
            }

            x11Support = ValidateUtils.checkNotNull(createX11ForwardSupport(session), "No X11 forwarder created for %s", session);
            x11ForwardHolder.set(x11Support);
        }
        return x11Support;
    }

    protected X11ForwardSupport createX11ForwardSupport(S session) {
        return new DefaultX11ForwardSupport(this);
    }

    @Override
    public AgentForwardSupport getAgentForwardSupport() {
        AgentForwardSupport agentForward;
        S session = getSession();
        synchronized (agentForwardHolder) {
            agentForward = agentForwardHolder.get();
            if (agentForward != null) {
                return agentForward;
            }

            agentForward = ValidateUtils.checkNotNull(createAgentForwardSupport(session), "No agent forward created for %s", session);
            agentForwardHolder.set(agentForward);
        }
        return agentForward;
    }

    protected AgentForwardSupport createAgentForwardSupport(S session) {
        return new DefaultAgentForwardSupport(this);
    }

    @Override
    protected Closeable getInnerCloseable() {
        return builder()
                .sequential(tcpipForwarderHolder.get(), agentForwardHolder.get(), x11ForwardHolder.get())
                .parallel(channels.values())
                .build();
    }

    protected int getNextChannelId() {
        return nextChannelId.getAndIncrement();
    }

    @Override
    public int registerChannel(Channel channel) throws IOException {
        Session session = getSession();
        int maxChannels = session.getIntProperty(MAX_CONCURRENT_CHANNELS_PROP, DEFAULT_MAX_CHANNELS);
        int curSize = channels.size();
        if (curSize > maxChannels) {
            throw new IllegalStateException("Currently active channels (" + curSize + ") at max.: " + maxChannels);
        }

        int channelId = getNextChannelId();
        channel.init(this, session, channelId);

        boolean registered = false;
        synchronized (lock) {
            if (!isClosing()) {
                channels.put(channelId, channel);
                registered = true;
            }
        }

        if (!registered) {
            handleChannelRegistrationFailure(channel, channelId);
        }
        return channelId;
    }

    protected void handleChannelRegistrationFailure(Channel channel, int channelId) throws IOException {
        RuntimeException reason = new IllegalStateException("Channel id=" + channelId + " not registered because session is being closed: " + this);
        AbstractChannel notifier =
                ValidateUtils.checkInstanceOf(channel, AbstractChannel.class, "Non abstract channel for id=%d", channelId);
        notifier.signalChannelClosed(reason);
        throw reason;
    }

    /**
     * Remove this channel from the list of managed channels
     *
     * @param channel the channel
     */
    @Override
    public void unregisterChannel(Channel channel) {
        Channel result = channels.remove(channel.getId());
    }

    @Override
    public void process(int cmd, Buffer buffer) throws Exception {
        switch (cmd) {
            case SshConstants.SSH_MSG_CHANNEL_OPEN:
                channelOpen(buffer);
                break;
            case SshConstants.SSH_MSG_CHANNEL_OPEN_CONFIRMATION:
                channelOpenConfirmation(buffer);
                break;
            case SshConstants.SSH_MSG_CHANNEL_OPEN_FAILURE:
                channelOpenFailure(buffer);
                break;
            case SshConstants.SSH_MSG_CHANNEL_REQUEST:
                channelRequest(buffer);
                break;
            case SshConstants.SSH_MSG_CHANNEL_DATA:
                channelData(buffer);
                break;
            case SshConstants.SSH_MSG_CHANNEL_EXTENDED_DATA:
                channelExtendedData(buffer);
                break;
            case SshConstants.SSH_MSG_CHANNEL_FAILURE:
                channelFailure(buffer);
                break;
            case SshConstants.SSH_MSG_CHANNEL_SUCCESS:
                channelSuccess(buffer);
                break;
            case SshConstants.SSH_MSG_CHANNEL_WINDOW_ADJUST:
                channelWindowAdjust(buffer);
                break;
            case SshConstants.SSH_MSG_CHANNEL_EOF:
                channelEof(buffer);
                break;
            case SshConstants.SSH_MSG_CHANNEL_CLOSE:
                channelClose(buffer);
                break;
            case SshConstants.SSH_MSG_GLOBAL_REQUEST:
                globalRequest(buffer);
                break;
            case SshConstants.SSH_MSG_REQUEST_SUCCESS:
                requestSuccess(buffer);
                break;
            case SshConstants.SSH_MSG_REQUEST_FAILURE:
                requestFailure(buffer);
                break;
            default:
                throw new IllegalStateException("Unsupported command: " + SshConstants.getCommandMessageName(cmd));
        }
    }

    @Override
    public boolean isAllowMoreSessions() {
        return allowMoreSessions.get();
    }

    @Override
    public void setAllowMoreSessions(boolean allow) {
        allowMoreSessions.set(allow);
    }

    public void channelOpenConfirmation(Buffer buffer) throws IOException {
        Channel channel = getChannel(buffer);
        int sender = buffer.getInt();
        long rwsize = buffer.getUInt();
        long rmpsize = buffer.getUInt();
        /*
         * NOTE: the 'sender' of the SSH_MSG_CHANNEL_OPEN_CONFIRMATION is the
         * recipient on the client side - see rfc4254 section 5.1:
         *
         *      'sender channel' is the channel number allocated by the other side
         *
         * in our case, the server
         */
        channel.handleOpenSuccess(sender, rwsize, rmpsize, buffer);
    }

    public void channelOpenFailure(Buffer buffer) throws IOException {
        AbstractClientChannel channel = (AbstractClientChannel) getChannel(buffer);
        int id = channel.getId();
        channels.remove(id);
        channel.handleOpenFailure(buffer);
    }

    /**
     * Process incoming data on a channel
     *
     * @param buffer the buffer containing the data
     * @throws IOException if an error occurs
     */
    public void channelData(Buffer buffer) throws IOException {
        Channel channel = getChannel(buffer);
        channel.handleData(buffer);
    }

    /**
     * Process incoming extended data on a channel
     *
     * @param buffer the buffer containing the data
     * @throws IOException if an error occurs
     */
    public void channelExtendedData(Buffer buffer) throws IOException {
        Channel channel = getChannel(buffer);
        channel.handleExtendedData(buffer);
    }

    /**
     * Process a window adjust packet on a channel
     *
     * @param buffer the buffer containing the window adjustment parameters
     * @throws IOException if an error occurs
     */
    public void channelWindowAdjust(Buffer buffer) throws IOException {
        try {
            Channel channel = getChannel(buffer);
            channel.handleWindowAdjust(buffer);
        } catch (SshException e) {
        }
    }

    /**
     * Process end of file on a channel
     *
     * @param buffer the buffer containing the packet
     * @throws IOException if an error occurs
     */
    public void channelEof(Buffer buffer) throws IOException {
        Channel channel = getChannel(buffer);
        channel.handleEof();
    }

    /**
     * Close a channel due to a close packet received
     *
     * @param buffer the buffer containing the packet
     * @throws IOException if an error occurs
     */
    public void channelClose(Buffer buffer) throws IOException {
        // Do not use getChannel to avoid the session being closed
        // if receiving the SSH_MSG_CHANNEL_CLOSE on an already closed channel
        int recipient = buffer.getInt();
        Channel channel = channels.get(recipient);
        if (channel != null) {
            channel.handleClose();
        } else {
        }
    }

    /**
     * Service a request on a channel
     *
     * @param buffer the buffer containing the request
     * @throws IOException if an error occurs
     */
    public void channelRequest(Buffer buffer) throws IOException {
        Channel channel = getChannel(buffer);
        channel.handleRequest(buffer);
    }

    /**
     * Process a failure on a channel
     *
     * @param buffer the buffer containing the packet
     * @throws IOException if an error occurs
     */
    public void channelFailure(Buffer buffer) throws IOException {
        Channel channel = getChannel(buffer);
        channel.handleFailure();
    }

    /**
     * Process a success on a channel
     *
     * @param buffer the buffer containing the packet
     * @throws IOException if an error occurs
     */
    public void channelSuccess(Buffer buffer) throws IOException {
        Channel channel = getChannel(buffer);
        channel.handleSuccess();
    }

    /**
     * Retrieve the channel designated by the given packet
     *
     * @param buffer the incoming packet
     * @return the target channel
     * @throws IOException if the channel does not exists
     */
    protected Channel getChannel(Buffer buffer) throws IOException {
        return getChannel(buffer.getInt(), buffer);
    }

    protected Channel getChannel(int recipient, Buffer buffer) throws IOException {
        Channel channel = channels.get(recipient);
        if (channel == null) {
            byte[] data = buffer.array();
            int curPos = buffer.rpos();
            int cmd = (curPos >= 5) ? (data[curPos - 5] & 0xFF) : -1;
            throw new SshException("Received " + SshConstants.getCommandMessageName(cmd) + " on unknown channel " + recipient);
        }

        return channel;
    }

    protected void channelOpen(Buffer buffer) throws Exception {
        String type = buffer.getString();
        final int sender = buffer.getInt();
        final long rwsize = buffer.getUInt();
        final long rmpsize = buffer.getUInt();
        /*
         * NOTE: the 'sender' is the identifier assigned by the remote side - the server in this case
         */

        if (isClosing()) {
            // TODO add language tag
            sendChannelOpenFailure(buffer, sender, SshConstants.SSH_OPEN_CONNECT_FAILED, "Server is shutting down while attempting to open channel type=" + type, "");
            return;
        }

        if (!isAllowMoreSessions()) {
            // TODO add language tag
            sendChannelOpenFailure(buffer, sender, SshConstants.SSH_OPEN_CONNECT_FAILED, "additional sessions disabled", "");
            return;
        }

        final Session session = getSession();
        FactoryManager manager = Objects.requireNonNull(session.getFactoryManager(), "No factory manager");
        final Channel channel = NamedFactory.create(manager.getChannelFactories(), type);
        if (channel == null) {
            // TODO add language tag
            sendChannelOpenFailure(buffer, sender, SshConstants.SSH_OPEN_UNKNOWN_CHANNEL_TYPE, "Unsupported channel type: " + type, "");
            return;
        }

        final int channelId = registerChannel(channel);
        channel.open(sender, rwsize, rmpsize, buffer).addListener(future -> {
            try {
                if (future.isOpened()) {
                    Window window = channel.getLocalWindow();
                    Buffer buf = session.createBuffer(SshConstants.SSH_MSG_CHANNEL_OPEN_CONFIRMATION, Integer.SIZE);
                    buf.putInt(sender); // remote (server side) identifier
                    buf.putInt(channelId);  // local (client side) identifier
                    buf.putInt(window.getSize());
                    buf.putInt(window.getPacketSize());
                    session.writePacket(buf);
                } else {
                    Throwable exception = future.getException();
                    if (exception != null) {
                        String message = exception.getMessage();
                        int reasonCode = 0;
                        if (exception instanceof OpenChannelException) {
                            reasonCode = ((OpenChannelException) exception).getReasonCode();
                        } else {
                            message = exception.getClass().getSimpleName() + " while opening channel: " + message;
                        }

                        Buffer buf = session.createBuffer(SshConstants.SSH_MSG_CHANNEL_OPEN_FAILURE, message.length() + Long.SIZE);
                        sendChannelOpenFailure(buf, sender, reasonCode, message, "");
                    }
                }
            } catch (IOException e) {
                session.exceptionCaught(e);
            }
        });
    }

    protected IoWriteFuture sendChannelOpenFailure(Buffer buffer, int sender, int reasonCode, String message, String lang) throws IOException {
        Session session = getSession();
        Buffer buf = session.createBuffer(SshConstants.SSH_MSG_CHANNEL_OPEN_FAILURE,
                Long.SIZE + GenericUtils.length(message) + GenericUtils.length(lang));
        buf.putInt(sender);
        buf.putInt(reasonCode);
        buf.putString(message);
        buf.putString(lang);
        return session.writePacket(buf);
    }

    /**
     * Process global requests
     *
     * @param buffer The request {@link Buffer}
     * @throws Exception If failed to process the request
     */
    protected void globalRequest(Buffer buffer) throws Exception {
        String req = buffer.getString();
        boolean wantReply = buffer.getBoolean();

        Session session = getSession();
        FactoryManager manager =
                Objects.requireNonNull(session.getFactoryManager(), "No factory manager");
        List<RequestHandler<ConnectionService>> handlers = manager.getGlobalRequestHandlers();
        if (GenericUtils.size(handlers) > 0) {
            for (RequestHandler<ConnectionService> handler : handlers) {
                RequestHandler.Result result;
                try {
                    result = handler.process(this, req, wantReply, buffer);
                } catch (Throwable e) {
                    result = RequestHandler.Result.ReplyFailure;
                }

                // if Unsupported then check the next handler in line
                if (RequestHandler.Result.Unsupported.equals(result)) {
                } else {
                    sendGlobalResponse(buffer, req, result, wantReply);
                    return;
                }
            }
        }

        handleUnknownRequest(buffer, req, wantReply);
    }

    protected void handleUnknownRequest(Buffer buffer, String req, boolean wantReply) throws IOException {
        sendGlobalResponse(buffer, req, RequestHandler.Result.Unsupported, wantReply);
    }

    protected IoWriteFuture sendGlobalResponse(Buffer buffer, String req, RequestHandler.Result result, boolean wantReply) throws IOException {
        if (RequestHandler.Result.Replied.equals(result) || (!wantReply)) {
            return new AbstractIoWriteFuture(null) {
                {
                    setValue(Boolean.TRUE);
                }
            };
        }

        byte cmd = RequestHandler.Result.ReplySuccess.equals(result)
                ? SshConstants.SSH_MSG_REQUEST_SUCCESS
                : SshConstants.SSH_MSG_REQUEST_FAILURE;
        Session session = getSession();
        Buffer rsp = session.createBuffer(cmd, 2);
        return session.writePacket(rsp);
    }

    protected void requestSuccess(Buffer buffer) throws Exception {
        getSession().requestSuccess(buffer);
    }

    protected void requestFailure(Buffer buffer) throws Exception {
        getSession().requestFailure(buffer);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getSession() + "]";
    }
}

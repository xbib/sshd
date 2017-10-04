package org.xbib.io.sshd.server.channel;

import org.xbib.io.sshd.common.agent.SshAgent;
import org.xbib.io.sshd.common.agent.SshAgentFactory;
import org.xbib.io.sshd.common.agent.AgentForwardSupport;
import org.xbib.io.sshd.common.Closeable;
import org.xbib.io.sshd.common.Factory;
import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.PropertyResolverUtils;
import org.xbib.io.sshd.common.RuntimeSshException;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.channel.ChannelAsyncOutputStream;
import org.xbib.io.sshd.common.channel.ChannelOutputStream;
import org.xbib.io.sshd.common.channel.ChannelRequestHandler;
import org.xbib.io.sshd.common.channel.PtyMode;
import org.xbib.io.sshd.common.channel.RequestHandler;
import org.xbib.io.sshd.common.channel.RequestHandler.Result;
import org.xbib.io.sshd.common.channel.Window;
import org.xbib.io.sshd.common.file.FileSystemAware;
import org.xbib.io.sshd.common.file.FileSystemFactory;
import org.xbib.io.sshd.common.forward.ForwardingFilter;
import org.xbib.io.sshd.common.future.CloseFuture;
import org.xbib.io.sshd.common.future.DefaultCloseFuture;
import org.xbib.io.sshd.common.future.SshFutureListener;
import org.xbib.io.sshd.common.io.IoWriteFuture;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;
import org.xbib.io.sshd.common.util.closeable.IoBaseCloseable;
import org.xbib.io.sshd.common.util.io.IoUtils;
import org.xbib.io.sshd.common.x11.X11ForwardSupport;
import org.xbib.io.sshd.server.AsyncCommand;
import org.xbib.io.sshd.server.ChannelSessionAware;
import org.xbib.io.sshd.server.Command;
import org.xbib.io.sshd.server.CommandFactory;
import org.xbib.io.sshd.server.Environment;
import org.xbib.io.sshd.server.ServerFactoryManager;
import org.xbib.io.sshd.server.SessionAware;
import org.xbib.io.sshd.server.Signal;
import org.xbib.io.sshd.server.StandardEnvironment;
import org.xbib.io.sshd.server.session.ServerSession;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class ChannelSession extends AbstractServerChannel {
    public static final List<ChannelRequestHandler> DEFAULT_HANDLERS =
            Collections.singletonList(PuttyRequestHandler.INSTANCE);
    protected final AtomicBoolean commandStarted = new AtomicBoolean(false);
    protected final StandardEnvironment env = new StandardEnvironment();
    protected final CloseFuture commandExitFuture = new DefaultCloseFuture(lock);
    protected String type;
    protected ChannelAsyncOutputStream asyncOut;
    protected ChannelAsyncOutputStream asyncErr;
    protected OutputStream out;
    protected OutputStream err;
    protected Command commandInstance;
    protected ChannelDataReceiver receiver;
    protected Buffer tempBuffer;

    public ChannelSession() {
        this(DEFAULT_HANDLERS);
    }

    public ChannelSession(Collection<? extends RequestHandler<Channel>> handlers) {
        super(handlers);
    }

    @Override
    public void handleWindowAdjust(Buffer buffer) throws IOException {
        super.handleWindowAdjust(buffer);
        if (asyncOut != null) {
            asyncOut.onWindowExpanded();
        }
    }

    @Override
    protected Closeable getInnerCloseable() {
        return builder()
                .sequential(new CommandCloseable(), new GracefulChannelCloseable())
                .parallel(asyncOut, asyncErr)
                .build();
    }

    @Override
    protected void doCloseImmediately() {
        if (commandInstance != null) {
            try {
                commandInstance.destroy();
            } catch (Throwable e) {
            } finally {
                commandInstance = null;
            }
        }

        IOException e = IoUtils.closeQuietly(getRemoteWindow(), out, err, receiver);
        if (e != null) {
        }

        super.doCloseImmediately();
    }

    @Override
    public void handleEof() throws IOException {
        super.handleEof();

        IOException e = IoUtils.closeQuietly(receiver);
        if (e != null) {
        }
    }

    @Override
    protected void doWriteData(byte[] data, int off, long len) throws IOException {
        // If we're already closing, ignore incoming data
        if (isClosing()) {
            return;
        }
        ValidateUtils.checkTrue(len <= Integer.MAX_VALUE, "Data length exceeds int boundaries: %d", len);

        if (receiver != null) {
            int r = receiver.data(this, data, off, (int) len);
            if (r > 0) {
                Window wLocal = getLocalWindow();
                wLocal.consumeAndCheck(r);
            }
        } else {
            ValidateUtils.checkTrue(len <= (Integer.MAX_VALUE - Long.SIZE), "Temporary data length exceeds int boundaries: %d", len);
            if (tempBuffer == null) {
                tempBuffer = new ByteArrayBuffer((int) len + Long.SIZE, false);
            }
            tempBuffer.putRawBytes(data, off, (int) len);
        }
    }

    @Override
    protected void doWriteExtendedData(byte[] data, int off, long len) throws IOException {
        throw new UnsupportedOperationException("Server channel does not support extended data");
    }

    @Override
    protected Result handleInternalRequest(String requestType, boolean wantReply, Buffer buffer) throws IOException {
        switch (requestType) {
            case "env":
                return handleEnv(buffer, wantReply);
            case "pty-req":
                return handlePtyReq(buffer, wantReply);
            case "window-change":
                return handleWindowChange(buffer, wantReply);
            case "signal":
                return handleSignal(buffer, wantReply);
            case "break":
                return handleBreak(buffer, wantReply);
            case Channel.CHANNEL_SHELL:
                if (this.type == null) {
                    Result r = handleShell(requestType, buffer, wantReply);
                    if (Result.ReplySuccess.equals(r) || Result.Replied.equals(r)) {
                        this.type = requestType;
                    }
                    return r;
                } else {
                    return Result.ReplyFailure;
                }
            case Channel.CHANNEL_EXEC:
                if (this.type == null) {
                    Result r = handleExec(requestType, buffer, wantReply);
                    if (Result.ReplySuccess.equals(r) || Result.Replied.equals(r)) {
                        this.type = requestType;
                    }
                    return r;
                } else {
                    return Result.ReplyFailure;
                }
            case Channel.CHANNEL_SUBSYSTEM:
                if (this.type == null) {
                    Result r = handleSubsystem(requestType, buffer, wantReply);
                    if (Result.ReplySuccess.equals(r) || Result.Replied.equals(r)) {
                        this.type = requestType;
                    }
                    return r;
                } else {
                    return Result.ReplyFailure;
                }
            case "auth-agent-req":  // see https://tools.ietf.org/html/draft-ietf-secsh-agent-02
            case "auth-agent-req@openssh.com":
                return handleAgentForwarding(requestType, buffer, wantReply);
            case "x11-req":
                return handleX11Forwarding(requestType, buffer, wantReply);
            default:
                return super.handleInternalRequest(requestType, wantReply, buffer);
        }
    }

    @Override
    protected IoWriteFuture sendResponse(Buffer buffer, String req, Result result, boolean wantReply) throws IOException {
        IoWriteFuture future = super.sendResponse(buffer, req, result, wantReply);
        if (!Result.ReplySuccess.equals(result)) {
            return future;
        }

        if (commandInstance == null) {
            return future; // no pending command to activate
        }

        if (!Objects.equals(this.type, req)) {
            return future; // request does not match the current channel type
        }

        if (commandStarted.getAndSet(true)) {
            return future;
        }

        // TODO - consider if (Channel.CHANNEL_SHELL.equals(req) || Channel.CHANNEL_EXEC.equals(req) || Channel.CHANNEL_SUBSYSTEM.equals(req)) {
        commandInstance.start(getEnvironment());
        return future;
    }

    protected Result handleEnv(Buffer buffer, boolean wantReply) throws IOException {
        String name = buffer.getString();
        String value = buffer.getString();
        addEnvVariable(name, value);
        return Result.ReplySuccess;
    }

    protected Result handlePtyReq(Buffer buffer, boolean wantReply) throws IOException {
        String term = buffer.getString();
        int tColumns = buffer.getInt();
        int tRows = buffer.getInt();
        int tWidth = buffer.getInt();
        int tHeight = buffer.getInt();
        byte[] modes = buffer.getBytes();
        Environment environment = getEnvironment();
        Map<PtyMode, Integer> ptyModes = environment.getPtyModes();

        for (int i = 0; (i < modes.length) && (modes[i] != PtyMode.TTY_OP_END); ) {
            int opcode = modes[i++] & 0x00FF;
            /*
             * According to https://tools.ietf.org/html/rfc4254#section-8:
             *
             *      Opcodes 160 to 255 are not yet defined, and cause parsing to stop
             */
            if ((opcode >= 160) && (opcode <= 255)) {
                break;
            }

            int val = ((modes[i++] << 24) & 0xff000000)
                    | ((modes[i++] << 16) & 0x00ff0000)
                    | ((modes[i++] << 8) & 0x0000ff00)
                    | ((modes[i++]) & 0x000000ff);
            PtyMode mode = PtyMode.fromInt(opcode);
            if (mode == null) {
            } else {
                ptyModes.put(mode, val);
            }
        }

        addEnvVariable(Environment.ENV_TERM, term);
        addEnvVariable(Environment.ENV_COLUMNS, Integer.toString(tColumns));
        addEnvVariable(Environment.ENV_LINES, Integer.toString(tRows));
        return Result.ReplySuccess;
    }

    protected Result handleWindowChange(Buffer buffer, boolean wantReply) throws IOException {
        int tColumns = buffer.getInt();
        int tRows = buffer.getInt();
        int tWidth = buffer.getInt();
        int tHeight = buffer.getInt();

        StandardEnvironment e = getEnvironment();
        e.set(Environment.ENV_COLUMNS, Integer.toString(tColumns));
        e.set(Environment.ENV_LINES, Integer.toString(tRows));
        e.signal(Signal.WINCH);
        return Result.ReplySuccess;
    }

    // see RFC4254 section 6.10
    protected Result handleSignal(Buffer buffer, boolean wantReply) throws IOException {
        String name = buffer.getString();

        Signal signal = Signal.get(name);
        if (signal != null) {
            getEnvironment().signal(signal);
        } else {
        }
        return Result.ReplySuccess;
    }

    // see rfc4335
    protected Result handleBreak(Buffer buffer, boolean wantReply) throws IOException {
        long breakLength = buffer.getUInt();

        getEnvironment().signal(Signal.INT);
        return Result.ReplySuccess;
    }

    protected Result handleShell(String request, Buffer buffer, boolean wantReply) throws IOException {
        // If we're already closing, ignore incoming data
        if (isClosing()) {
            return Result.ReplyFailure;
        }

        ServerFactoryManager manager = Objects.requireNonNull(getServerSession(), "No server session").getFactoryManager();
        Factory<Command> factory = Objects.requireNonNull(manager, "No server factory manager").getShellFactory();
        if (factory == null) {
            return Result.ReplyFailure;
        }

        try {
            commandInstance = factory.create();
        } catch (RuntimeException | Error e) {
            return Result.ReplyFailure;
        }

        if (commandInstance == null) {
            return Result.ReplyFailure;
        }

        return prepareChannelCommand(request, commandInstance);
    }

    protected Result handleExec(String request, Buffer buffer, boolean wantReply) throws IOException {
        // If we're already closing, ignore incoming data
        if (isClosing()) {
            return Result.ReplyFailure;
        }

        String commandLine = buffer.getString();
        ServerFactoryManager manager = Objects.requireNonNull(getServerSession(), "No server session").getFactoryManager();
        CommandFactory factory = Objects.requireNonNull(manager, "No server factory manager").getCommandFactory();
        if (factory == null) {
            return Result.ReplyFailure;
        }

        try {
            commandInstance = factory.createCommand(commandLine);
        } catch (RuntimeException | Error e) {
            return Result.ReplyFailure;
        }

        if (commandInstance == null) {
            return Result.ReplyFailure;
        }

        return prepareChannelCommand(request, commandInstance);
    }

    protected Result handleSubsystem(String request, Buffer buffer, boolean wantReply) throws IOException {
        String subsystem = buffer.getString();

        ServerFactoryManager manager = Objects.requireNonNull(getServerSession(), "No server session").getFactoryManager();
        List<NamedFactory<Command>> factories = Objects.requireNonNull(manager, "No server factory manager").getSubsystemFactories();
        if (GenericUtils.isEmpty(factories)) {
            return Result.ReplyFailure;
        }

        try {
            commandInstance = NamedFactory.create(factories, subsystem);
        } catch (RuntimeException | Error e) {
            return Result.ReplyFailure;
        }

        if (commandInstance == null) {
            return Result.ReplyFailure;
        }

        return prepareChannelCommand(request, commandInstance);
    }

    protected Result prepareChannelCommand(String request, Command cmd) throws IOException {
        Command command = prepareCommand(request, cmd);
        if (command == null) {
            return Result.ReplyFailure;
        }

        if (command != cmd) {
            commandInstance = command;
        }

        return Result.ReplySuccess;
    }

    /**
     * For {@link Command} to install {@link ChannelDataReceiver}.
     * When you do this, {@link Command#setInputStream(java.io.InputStream)} or
     * {@link AsyncCommand#setIoInputStream(org.xbib.io.sshd.common.io.IoInputStream)}
     * will no longer be invoked. If you call this method from {@link Command#start(Environment)},
     * the input stream you received in {@link Command#setInputStream(java.io.InputStream)} will
     * not read any data.
     *
     * @param receiver The {@link ChannelDataReceiver} instance
     */
    public void setDataReceiver(ChannelDataReceiver receiver) {
        this.receiver = receiver;
    }

    /**
     * Called by {@link #prepareChannelCommand(String, Command)} in order to set
     * up the command's streams, session, file-system, exit callback, etc..
     *
     * @param requestType The request that caused the command to be created
     * @param command     The created {@link Command} - may be {@code null}
     * @return The updated command instance - if {@code null} then the request that
     * initially caused the creation of the command is failed and the original command
     * (if any) destroyed (eventually). <B>Note:</B> if a different command instance
     * than the input one is returned, then it is up to the implementor to take care
     * of the wrapping or destruction of the original command instance.
     * @throws IOException If failed to prepare the command
     */
    protected Command prepareCommand(String requestType, Command command) throws IOException {
        if (command == null) {
            return null;
        }
        // Add the user
        Session session = getSession();
        addEnvVariable(Environment.ENV_USER, session.getUsername());
        // If the shell wants to be aware of the session, let's do that
        if (command instanceof SessionAware) {
            ((SessionAware) command).setSession((ServerSession) session);
        }
        if (command instanceof ChannelSessionAware) {
            ((ChannelSessionAware) command).setChannelSession(this);
        }
        // If the shell wants to be aware of the file system, let's do that too
        if (command instanceof FileSystemAware) {
            ServerFactoryManager manager = ((ServerSession) session).getFactoryManager();
            FileSystemFactory factory = manager.getFileSystemFactory();
            ((FileSystemAware) command).setFileSystem(factory.createFileSystem(session));
        }
        // If the shell wants to use non-blocking io
        if (command instanceof AsyncCommand) {
            asyncOut = new ChannelAsyncOutputStream(this, SshConstants.SSH_MSG_CHANNEL_DATA);
            asyncErr = new ChannelAsyncOutputStream(this, SshConstants.SSH_MSG_CHANNEL_EXTENDED_DATA);
            ((AsyncCommand) command).setIoOutputStream(asyncOut);
            ((AsyncCommand) command).setIoErrorStream(asyncErr);
        } else {
            Window wRemote = getRemoteWindow();
            out = new ChannelOutputStream(this, wRemote, SshConstants.SSH_MSG_CHANNEL_DATA, false);
            err = new ChannelOutputStream(this, wRemote, SshConstants.SSH_MSG_CHANNEL_EXTENDED_DATA, false);
            command.setOutputStream(out);
            command.setErrorStream(err);
        }
        if (this.receiver == null) {
            // if the command hasn't installed any ChannelDataReceiver, install the default
            // and give the command an InputStream
            if (command instanceof AsyncCommand) {
                AsyncDataReceiver recv = new AsyncDataReceiver(this);
                setDataReceiver(recv);
                ((AsyncCommand) command).setIoInputStream(recv.getIn());
            } else {
                PipeDataReceiver recv = new PipeDataReceiver(this, getLocalWindow());
                setDataReceiver(recv);
                command.setInputStream(recv.getIn());
            }
        }
        if (tempBuffer != null) {
            Buffer buffer = tempBuffer;
            tempBuffer = null;
            doWriteData(buffer.array(), buffer.rpos(), buffer.available());
        }
        command.setExitCallback((exitValue, exitMessage) -> {
            try {
                closeShell(exitValue);
            } catch (IOException e) {
            }
        });

        return command;
    }

    protected int getPtyModeValue(PtyMode mode) {
        Number v = getEnvironment().getPtyModes().get(mode);
        return v != null ? v.intValue() : 0;
    }

    protected Result handleAgentForwarding(String requestType, Buffer buffer, boolean wantReply) throws IOException {
        ServerSession session = getServerSession();
        FactoryManager manager = Objects.requireNonNull(session.getFactoryManager(), "No session factory manager");
        ForwardingFilter filter = manager.getTcpipForwardingFilter();
        SshAgentFactory factory = manager.getAgentFactory();
        try {
            if ((factory == null) || (filter == null) || (!filter.canForwardAgent(session, requestType))) {
                return Result.ReplyFailure;
            }
        } catch (Error e) {
            throw new RuntimeSshException(e);
        }

        AgentForwardSupport agentForward = service.getAgentForwardSupport();
        if (agentForward == null) {
            return Result.ReplyFailure;
        }

        String authSocket = agentForward.initialize();
        addEnvVariable(SshAgent.SSH_AUTHSOCKET_ENV_NAME, authSocket);
        return Result.ReplySuccess;
    }

    protected Result handleX11Forwarding(String requestType, Buffer buffer, boolean wantReply) throws IOException {
        ServerSession session = getServerSession();
        boolean singleConnection = buffer.getBoolean();
        String authProtocol = buffer.getString();
        String authCookie = buffer.getString();
        int screenId = buffer.getInt();

        FactoryManager manager = Objects.requireNonNull(session.getFactoryManager(), "No factory manager");
        ForwardingFilter filter = manager.getTcpipForwardingFilter();
        try {
            if ((filter == null) || (!filter.canForwardX11(session, requestType))) {
                return Result.ReplyFailure;
            }
        } catch (Error e) {
            throw new RuntimeSshException(e);
        }

        X11ForwardSupport x11Forward = service.getX11ForwardSupport();
        if (x11Forward == null) {
            return Result.ReplyFailure;
        }

        String display = x11Forward.createDisplay(singleConnection, authProtocol, authCookie, screenId);
        if (GenericUtils.isEmpty(display)) {
            return Result.ReplyFailure;
        }

        addEnvVariable(X11ForwardSupport.ENV_DISPLAY, display);
        return Result.ReplySuccess;
    }

    protected void addEnvVariable(String name, String value) {
        getEnvironment().set(name, value);
    }

    public StandardEnvironment getEnvironment() {
        return env;
    }

    protected void closeShell(int exitValue) throws IOException {
        if (!isClosing()) {
            sendEof();
            sendExitStatus(exitValue);
            commandExitFuture.setClosed();
            close(false);
        } else {
            commandExitFuture.setClosed();
        }
    }

    public class CommandCloseable extends IoBaseCloseable {
        public CommandCloseable() {
            super();
        }

        @Override
        public boolean isClosed() {
            return commandExitFuture.isClosed();
        }

        @Override
        public boolean isClosing() {
            return isClosed();
        }

        @Override
        public void addCloseFutureListener(SshFutureListener<CloseFuture> listener) {
            commandExitFuture.addListener(listener);
        }

        @Override
        public void removeCloseFutureListener(SshFutureListener<CloseFuture> listener) {
            commandExitFuture.removeListener(listener);
        }

        @Override
        public CloseFuture close(boolean immediately) {
            if (immediately || (commandInstance == null)) {
                commandExitFuture.setClosed();
            } else if (!commandExitFuture.isClosed()) {
                IOException e = IoUtils.closeQuietly(receiver);
                if (e != null) {
                }

                final TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        commandExitFuture.setClosed();
                    }
                };

                ChannelSession channel = ChannelSession.this;
                long timeout = PropertyResolverUtils.getLongProperty(
                        channel, ServerFactoryManager.COMMAND_EXIT_TIMEOUT, ServerFactoryManager.DEFAULT_COMMAND_EXIT_TIMEOUT);

                Session s = channel.getSession();
                FactoryManager manager = Objects.requireNonNull(s.getFactoryManager(), "No factory manager");
                ScheduledExecutorService scheduler = Objects.requireNonNull(manager.getScheduledExecutorService(), "No scheduling service");
                scheduler.schedule(task, timeout, TimeUnit.MILLISECONDS);
                commandExitFuture.addListener(future -> task.cancel());
            }
            return commandExitFuture;
        }
    }
}

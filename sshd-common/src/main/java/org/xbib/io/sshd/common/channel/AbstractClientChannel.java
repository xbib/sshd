package org.xbib.io.sshd.common.channel;

import org.xbib.io.sshd.common.Closeable;
import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.channel.exit.ExitSignalChannelRequestHandler;
import org.xbib.io.sshd.common.channel.exit.ExitStatusChannelRequestHandler;
import org.xbib.io.sshd.common.future.DefaultOpenFuture;
import org.xbib.io.sshd.common.future.OpenFuture;
import org.xbib.io.sshd.common.io.IoInputStream;
import org.xbib.io.sshd.common.io.IoOutputStream;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.EventNotifier;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;
import org.xbib.io.sshd.common.util.io.IoUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public abstract class AbstractClientChannel extends AbstractChannel implements ClientChannel {

    protected final AtomicBoolean opened = new AtomicBoolean();
    protected final String type;
    protected final AtomicReference<Integer> exitStatusHolder = new AtomicReference<>(null);
    protected final AtomicReference<String> exitSignalHolder = new AtomicReference<>(null);
    protected Streaming streaming;
    protected ChannelAsyncOutputStream asyncIn;
    protected ChannelAsyncInputStream asyncOut;
    protected ChannelAsyncInputStream asyncErr;
    protected InputStream in;
    protected OutputStream invertedIn;
    protected OutputStream out;
    protected InputStream invertedOut;
    protected OutputStream err;
    protected InputStream invertedErr;
    protected int openFailureReason;
    protected String openFailureMsg;
    protected String openFailureLang;
    protected OpenFuture openFuture;

    protected AbstractClientChannel(String type) {
        this(type, Collections.emptyList());
    }

    protected AbstractClientChannel(String type, Collection<? extends RequestHandler<Channel>> handlers) {
        super(true, handlers);
        this.type = ValidateUtils.checkNotNullAndNotEmpty(type, "No channel type specified");
        this.streaming = Streaming.Sync;

        addChannelSignalRequestHandlers(event -> {
            notifyStateChanged(event);
        });
    }

    protected void addChannelSignalRequestHandlers(EventNotifier<String> notifier) {
        addRequestHandler(new ExitStatusChannelRequestHandler(exitStatusHolder, notifier));
        addRequestHandler(new ExitSignalChannelRequestHandler(exitSignalHolder, notifier));
    }

    @Override
    public Streaming getStreaming() {
        return streaming;
    }

    @Override
    public void setStreaming(Streaming streaming) {
        this.streaming = streaming;
    }

    @Override
    public IoOutputStream getAsyncIn() {
        return asyncIn;
    }

    @Override
    public IoInputStream getAsyncOut() {
        return asyncOut;
    }

    @Override
    public IoInputStream getAsyncErr() {
        return asyncErr;
    }

    @Override
    public OutputStream getInvertedIn() {
        return invertedIn;
    }

    public InputStream getIn() {
        return in;
    }

    @Override
    public void setIn(InputStream in) {
        this.in = in;
    }

    @Override
    public InputStream getInvertedOut() {
        return invertedOut;
    }

    public OutputStream getOut() {
        return out;
    }

    @Override
    public void setOut(OutputStream out) {
        this.out = out;
    }

    @Override
    public InputStream getInvertedErr() {
        return invertedErr;
    }

    public OutputStream getErr() {
        return err;
    }

    @Override
    public void setErr(OutputStream err) {
        this.err = err;
    }

    @Override
    protected Closeable getInnerCloseable() {
        return builder()
                .when(openFuture)
                .run(() -> {
                    // If the channel has not been opened yet,
                    // skip the SSH_MSG_CHANNEL_CLOSE exchange
                    if (openFuture == null) {
                        gracefulFuture.setClosed();
                    }
                    // Close inverted streams after
                    // If the inverted stream is closed before, there's a small time window
                    // in which we have:
                    //    ChannelPipedInputStream#closed = true
                    //    ChannelPipedInputStream#writerClosed = false
                    // which leads to an IOException("Pipe closed") when reading.
                    IoUtils.closeQuietly(in, out, err);
                    IoUtils.closeQuietly(invertedIn, invertedOut, invertedErr);
                })
                .parallel(asyncIn, asyncOut, asyncErr)
                .close(new GracefulChannelCloseable())
                .build();
    }

    @Override
    public Set<ClientChannelEvent> waitFor(Collection<ClientChannelEvent> mask, long timeout) {
        Objects.requireNonNull(mask, "No mask specified");
        long t = 0;
        synchronized (lock) {
            for (Set<ClientChannelEvent> cond = EnumSet.noneOf(ClientChannelEvent.class); ; cond.clear()) {
                updateCurrentChannelState(cond);

                boolean nothingInCommon = Collections.disjoint(mask, cond);
                if (!nothingInCommon) {
                    return cond;
                }

                if (timeout > 0L) {
                    if (t == 0L) {
                        t = System.currentTimeMillis() + timeout;
                    } else {
                        timeout = t - System.currentTimeMillis();
                        if (timeout <= 0L) {
                            cond.add(ClientChannelEvent.TIMEOUT);
                            return cond;
                        }
                    }
                }
                long nanoStart = System.nanoTime();
                try {
                    if (timeout > 0L) {
                        lock.wait(timeout);
                    } else {
                        lock.wait();
                    }

                    long nanoEnd = System.nanoTime();
                    long nanoDuration = nanoEnd - nanoStart;
                } catch (InterruptedException e) {
                    long nanoEnd = System.nanoTime();
                    long nanoDuration = nanoEnd - nanoStart;
                }
            }
        }
    }

    @Override
    public Set<ClientChannelEvent> getChannelState() {
        Set<ClientChannelEvent> cond = EnumSet.noneOf(ClientChannelEvent.class);
        synchronized (lock) {
            return updateCurrentChannelState(cond);
        }
    }

    // NOTE: assumed to be called under lock
    protected <C extends Collection<ClientChannelEvent>> C updateCurrentChannelState(C state) {
        if ((openFuture != null) && openFuture.isOpened()) {
            state.add(ClientChannelEvent.OPENED);
        }
        if (closeFuture.isClosed()) {
            state.add(ClientChannelEvent.CLOSED);
        }
        if (isEofSignalled()) {
            state.add(ClientChannelEvent.EOF);
        }
        if (exitStatusHolder.get() != null) {
            state.add(ClientChannelEvent.EXIT_STATUS);
        }
        if (exitSignalHolder.get() != null) {
            state.add(ClientChannelEvent.EXIT_SIGNAL);
        }

        return state;
    }

    @Override
    public synchronized OpenFuture open() throws IOException {
        if (isClosing()) {
            throw new SshException("Session has been closed");
        }

        openFuture = new DefaultOpenFuture(lock);

        Session session = getSession();
        Window wLocal = getLocalWindow();
        Buffer buffer = session.createBuffer(SshConstants.SSH_MSG_CHANNEL_OPEN, type.length() + Integer.SIZE);
        buffer.putString(type);
        buffer.putInt(getId());
        buffer.putInt(wLocal.getSize());
        buffer.putInt(wLocal.getPacketSize());
        writePacket(buffer);
        return openFuture;
    }

    @Override
    public OpenFuture open(int recipient, long rwSize, long packetSize, Buffer buffer) {
        throw new UnsupportedOperationException("open(" + recipient + "," + rwSize + "," + packetSize + ") N/A");
    }

    @Override
    public void handleOpenSuccess(int recipient, long rwSize, long packetSize, Buffer buffer) {
        setRecipient(recipient);

        Session session = getSession();
        FactoryManager manager = Objects.requireNonNull(session.getFactoryManager(), "No factory manager");
        Window wRemote = getRemoteWindow();
        wRemote.init(rwSize, packetSize, manager);

        String changeEvent = "SSH_MSG_CHANNEL_OPEN_CONFIRMATION";
        try {
            doOpen();

            signalChannelOpenSuccess();
            this.opened.set(true);
            this.openFuture.setOpened();
        } catch (Throwable t) {
            Throwable e = GenericUtils.peelException(t);
            changeEvent = e.getClass().getName();
            signalChannelOpenFailure(e);
            this.openFuture.setException(e);
            this.closeFuture.setClosed();
            this.doCloseImmediately();
        } finally {
            notifyStateChanged(changeEvent);
        }
    }

    protected abstract void doOpen() throws IOException;

    @Override
    public void handleOpenFailure(Buffer buffer) {
        int reason = buffer.getInt();
        String msg = buffer.getString();
        String lang = buffer.getString();
        this.openFailureReason = reason;
        this.openFailureMsg = msg;
        this.openFailureLang = lang;
        this.openFuture.setException(new SshException(msg));
        this.closeFuture.setClosed();
        this.doCloseImmediately();
        notifyStateChanged("SSH_MSG_CHANNEL_OPEN_FAILURE");
    }

    @Override
    protected void doWriteData(byte[] data, int off, long len) throws IOException {
        // If we're already closing, ignore incoming data
        if (isClosing()) {
            return;
        }
        ValidateUtils.checkTrue(len <= Integer.MAX_VALUE, "Data length exceeds int boundaries: %d", len);

        if (asyncOut != null) {
            asyncOut.write(new ByteArrayBuffer(data, off, (int) len));
        } else if (out != null) {
            out.write(data, off, (int) len);
            out.flush();

            if (invertedOut == null) {
                Window wLocal = getLocalWindow();
                wLocal.consumeAndCheck(len);
            }
        } else {
            throw new IllegalStateException("No output stream for channel");
        }
    }

    @Override
    protected void doWriteExtendedData(byte[] data, int off, long len) throws IOException {
        // If we're already closing, ignore incoming data
        if (isClosing()) {
            return;
        }
        ValidateUtils.checkTrue(len <= Integer.MAX_VALUE, "Extended data length exceeds int boundaries: %d", len);

        if (asyncErr != null) {
            asyncErr.write(new ByteArrayBuffer(data, off, (int) len));
        } else if (err != null) {
            err.write(data, off, (int) len);
            err.flush();

            if (invertedErr == null) {
                Window wLocal = getLocalWindow();
                wLocal.consumeAndCheck(len);
            }
        } else {
            throw new IllegalStateException("No error stream for channel");
        }
    }

    @Override
    public void handleWindowAdjust(Buffer buffer) throws IOException {
        super.handleWindowAdjust(buffer);
        if (asyncIn != null) {
            asyncIn.onWindowExpanded();
        }
    }

    @Override
    public Integer getExitStatus() {
        return exitStatusHolder.get();
    }

    @Override
    public String getExitSignal() {
        return exitSignalHolder.get();
    }
}

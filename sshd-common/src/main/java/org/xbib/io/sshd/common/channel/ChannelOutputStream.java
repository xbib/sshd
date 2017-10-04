package org.xbib.io.sshd.common.channel;

import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 */
public class ChannelOutputStream extends OutputStream implements java.nio.channels.Channel, ChannelHolder {
    /**
     * Configure max. wait time (millis) to wait for space to become available
     */
    public static final String WAIT_FOR_SPACE_TIMEOUT = "channel-output-wait-for-space-timeout";
    public static final long DEFAULT_WAIT_FOR_SPACE_TIMEOUT = TimeUnit.SECONDS.toMillis(30L);

    private final AbstractChannel channelInstance;
    private final Window remoteWindow;
    private final long maxWaitTimeout;
    private final byte cmd;
    private final boolean eofOnClose;
    private final byte[] b = new byte[1];
    private final AtomicBoolean closedState = new AtomicBoolean(false);
    private Buffer buffer;
    private int bufferLength;
    private int lastSize;
    private boolean noDelay;

    public ChannelOutputStream(AbstractChannel channel, Window remoteWindow, byte cmd, boolean eofOnClose) {
        this(channel, remoteWindow, channel.getLongProperty(WAIT_FOR_SPACE_TIMEOUT, DEFAULT_WAIT_FOR_SPACE_TIMEOUT), cmd, eofOnClose);
    }

    public ChannelOutputStream(AbstractChannel channel, Window remoteWindow, long maxWaitTimeout, byte cmd, boolean eofOnClose) {
        this.channelInstance = Objects.requireNonNull(channel, "No channel");
        this.remoteWindow = Objects.requireNonNull(remoteWindow, "No remote window");
        ValidateUtils.checkTrue(maxWaitTimeout > 0L, "Non-positive max. wait time: %d", maxWaitTimeout);
        this.maxWaitTimeout = maxWaitTimeout;
        this.cmd = cmd;
        this.eofOnClose = eofOnClose;
        newBuffer(0);
    }

    @Override   // co-variant return
    public AbstractChannel getChannel() {
        return channelInstance;
    }

    public boolean isEofOnClose() {
        return eofOnClose;
    }

    public boolean isNoDelay() {
        return noDelay;
    }

    public void setNoDelay(boolean noDelay) {
        this.noDelay = noDelay;
    }

    @Override
    public boolean isOpen() {
        return !closedState.get();
    }

    @Override
    public synchronized void write(int w) throws IOException {
        b[0] = (byte) w;
        write(b, 0, 1);
    }

    @Override
    public synchronized void write(byte[] buf, int s, int l) throws IOException {
        if (!isOpen()) {
            throw new SshException("write(" + this + ") len=" + l + " - channel already closed");
        }

        Channel channel = getChannel();
        Session session = channel.getSession();
        while (l > 0) {
            // The maximum amount we should admit without flushing again
            // is enough to make up one full packet within our allowed
            // window size.  We give ourselves a credit equal to the last
            // packet we sent to allow the producer to race ahead and fill
            // out the next packet before we block and wait for space to
            // become available again.
            long l2 = Math.min(l, Math.min(remoteWindow.getSize() + lastSize, remoteWindow.getPacketSize()) - bufferLength);
            if (l2 <= 0) {
                if (bufferLength > 0) {
                    flush();
                } else {
                    session.resetIdleTimeout();
                    try {
                        long available = remoteWindow.waitForSpace(maxWaitTimeout);
                    } catch (IOException e) {

                        if ((e instanceof WindowClosedException) && (!closedState.getAndSet(true))) {
                        }

                        throw e;
                    } catch (InterruptedException e) {
                        throw (IOException) new InterruptedIOException("Interrupted while waiting for remote space on write len=" + l + " to " + this).initCause(e);
                    }
                }
                session.resetIdleTimeout();
                continue;
            }

            ValidateUtils.checkTrue(l2 <= Integer.MAX_VALUE, "Accumulated bytes length exceeds int boundary: %d", l2);
            buffer.putRawBytes(buf, s, (int) l2);
            bufferLength += l2;
            s += l2;
            l -= l2;
        }

        if (isNoDelay()) {
            flush();
        } else {
            session.resetIdleTimeout();
        }
    }

    @Override
    public synchronized void flush() throws IOException {
        if (!isOpen()) {
            throw new SshException("flush(" + this + ") length=" + bufferLength + " - stream is already closed");
        }

        try {
            AbstractChannel channel = getChannel();
            Session session = channel.getSession();
            while (bufferLength > 0) {
                session.resetIdleTimeout();

                Buffer buf = buffer;
                long total = bufferLength;
                long available;
                try {
                    available = remoteWindow.waitForSpace(maxWaitTimeout);
                } catch (IOException e) {

                    throw e;
                }

                long lenToSend = Math.min(available, total);
                long length = Math.min(lenToSend, remoteWindow.getPacketSize());
                if (length > Integer.MAX_VALUE) {
                    throw new StreamCorruptedException("Accumulated " + SshConstants.getCommandMessageName(cmd)
                            + " command bytes size (" + length + ") exceeds int boundaries");
                }

                int pos = buf.wpos();
                buf.wpos((cmd == SshConstants.SSH_MSG_CHANNEL_EXTENDED_DATA) ? 14 : 10);
                buf.putInt(length);
                buf.wpos(buf.wpos() + (int) length);
                if (total == length) {
                    newBuffer((int) length);
                } else {
                    long leftover = total - length;
                    newBuffer((int) Math.max(leftover, length));
                    buffer.putRawBytes(buf.array(), pos - (int) leftover, (int) leftover);
                    bufferLength = (int) leftover;
                }
                lastSize = (int) length;

                session.resetIdleTimeout();
                remoteWindow.waitAndConsume(length, maxWaitTimeout);
                channel.writePacket(buf);
            }
        } catch (WindowClosedException e) {
            if (!closedState.getAndSet(true)) {
            }
            throw e;
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            } else if (e instanceof InterruptedException) {
                throw (IOException) new InterruptedIOException("Interrupted while waiting for remote space flush len=" + bufferLength + " to " + this).initCause(e);
            } else {
                throw new SshException(e);
            }
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (isOpen()) {

            try {
                flush();

                if (isEofOnClose()) {
                    AbstractChannel channel = getChannel();
                    channel.sendEof();
                }
            } finally {
                closedState.set(true);
            }
        }
    }

    protected void newBuffer(int size) {
        Channel channel = getChannel();
        Session session = channel.getSession();
        buffer = session.createBuffer(cmd, size <= 0 ? 12 : 12 + size);
        buffer.putInt(channel.getRecipient());
        if (cmd == SshConstants.SSH_MSG_CHANNEL_EXTENDED_DATA) {
            buffer.putInt(SshConstants.SSH_EXTENDED_DATA_STDERR);
        }
        buffer.putInt(0);
        bufferLength = 0;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getChannel() + "] " + SshConstants.getCommandMessageName(cmd & 0xFF);
    }
}

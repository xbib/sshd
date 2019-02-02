package org.xbib.io.sshd.common.channel;

import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.future.CloseFuture;
import org.xbib.io.sshd.common.future.SshFutureListener;
import org.xbib.io.sshd.common.io.IoOutputStream;
import org.xbib.io.sshd.common.io.IoWriteFuture;
import org.xbib.io.sshd.common.io.WritePendingException;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.closeable.AbstractCloseable;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class ChannelAsyncOutputStream extends AbstractCloseable implements IoOutputStream, ChannelHolder {

    private final Channel channelInstance;
    private final byte cmd;
    private final AtomicReference<IoWriteFutureImpl> pendingWrite = new AtomicReference<>();
    private final Object packetWriteId;

    public ChannelAsyncOutputStream(Channel channel, byte cmd) {
        this.channelInstance = Objects.requireNonNull(channel, "No channel");
        this.cmd = cmd;
        this.packetWriteId = channel.toString() + "[" +SshConstants.getCommandMessageName(cmd) + "]";
    }

    @Override
    public Channel getChannel() {
        return channelInstance;
    }

    public void onWindowExpanded() throws IOException {
        doWriteIfPossible(true);
    }

    @Override
    public synchronized IoWriteFuture write(final Buffer buffer) {
        final IoWriteFutureImpl future = new IoWriteFutureImpl(packetWriteId, buffer);
        if (isClosing()) {
            future.setValue(new IOException("Closed"));
        } else {
            if (!pendingWrite.compareAndSet(null, future)) {
                throw new WritePendingException("No write pending future");
            }
            doWriteIfPossible(false);
        }
        return future;
    }

    @Override
    protected CloseFuture doCloseGracefully() {
        return builder().when(pendingWrite.get()).build().close(false);
    }

    protected synchronized void doWriteIfPossible(boolean resume) {
        IoWriteFutureImpl future = pendingWrite.get();
        if (future == null) {
            return;
        }

        Buffer buffer = future.getBuffer();
        int total = buffer.available();
        if (total > 0) {
            Channel channel = getChannel();
            Window remoteWindow = channel.getRemoteWindow();
            long length = Math.min(Math.min(remoteWindow.getSize(), total), remoteWindow.getPacketSize());
            if (length > 0) {
                if (resume) {
                }

                if (length >= (Integer.MAX_VALUE - 12)) {
                    throw new IllegalArgumentException("Command " + SshConstants.getCommandMessageName(cmd) + " length (" + length + " exceeds int boundaries");
                }
                Session s = channel.getSession();

                Buffer buf = s.createBuffer(cmd, (int) length + 12);
                buf.putInt(channel.getRecipient());
                if (cmd == SshConstants.SSH_MSG_CHANNEL_EXTENDED_DATA) {
                    buf.putInt(SshConstants.SSH_EXTENDED_DATA_STDERR);
                }
                buf.putInt(length);
                buf.putRawBytes(buffer.array(), buffer.rpos(), (int) length);
                buffer.rpos(buffer.rpos() + (int) length);
                remoteWindow.consume(length);
                try {
                    final ChannelAsyncOutputStream stream = this;
                    s.writePacket(buf).addListener(new SshFutureListener<IoWriteFuture>() {
                        @Override
                        public void operationComplete(IoWriteFuture f) {
                            if (f.isWritten()) {
                                handleOperationCompleted();
                            } else {
                                handleOperationFailed(f.getException());
                            }
                        }

                        @SuppressWarnings("synthetic-access")
                        private void handleOperationCompleted() {
                            if (total > length) {
                                doWriteIfPossible(false);
                            } else {
                                boolean nullified = pendingWrite.compareAndSet(future, null);
                                future.setValue(Boolean.TRUE);
                            }
                        }

                        @SuppressWarnings("synthetic-access")
                        private void handleOperationFailed(Throwable reason) {

                            boolean nullified = pendingWrite.compareAndSet(future, null);
                            future.setValue(reason);
                        }
                    });
                } catch (IOException e) {
                    future.setValue(e);
                }
            } else if (!resume) {
            }
        } else {
            boolean nullified = pendingWrite.compareAndSet(future, null);
            future.setValue(Boolean.TRUE);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getChannel() + "] cmd=" + SshConstants.getCommandMessageName(cmd & 0xFF);
    }
}

package org.xbib.io.sshd.common.channel;

import org.xbib.io.sshd.common.RuntimeSshException;
import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.future.CloseFuture;
import org.xbib.io.sshd.common.future.DefaultVerifiableSshFuture;
import org.xbib.io.sshd.common.io.IoInputStream;
import org.xbib.io.sshd.common.io.IoReadFuture;
import org.xbib.io.sshd.common.io.ReadPendingException;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.Readable;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;
import org.xbib.io.sshd.common.util.closeable.AbstractCloseable;

import java.io.IOException;
import java.util.Objects;

/**
 *
 */
public class ChannelAsyncInputStream extends AbstractCloseable implements IoInputStream, ChannelHolder {
    private final org.xbib.io.sshd.common.channel.Channel channelInstance;
    private final Buffer buffer = new ByteArrayBuffer();
    private IoReadFutureImpl pending;

    public ChannelAsyncInputStream(org.xbib.io.sshd.common.channel.Channel channel) {
        this.channelInstance = Objects.requireNonNull(channel, "No channel");
    }

    @Override
    public org.xbib.io.sshd.common.channel.Channel getChannel() {
        return channelInstance;
    }

    public void write(Readable src) throws IOException {
        synchronized (buffer) {
            buffer.putBuffer(src);
        }
        doRead(true);
    }

    @Override
    public IoReadFuture read(Buffer buf) {
        IoReadFutureImpl future = new IoReadFutureImpl(buf);
        if (isClosing()) {
            future.setValue(new IOException("Closed"));
        } else {
            synchronized (buffer) {
                if (pending != null) {
                    throw new ReadPendingException("Previous pending read not handled");
                }
                pending = future;
            }
            doRead(false);
        }
        return future;
    }

    @Override
    protected void preClose() {
        synchronized (buffer) {
            if (buffer.available() == 0) {
                if (pending != null) {
                    pending.setValue(new SshException("Closed"));
                }
            }
        }
        super.preClose();
    }

    @Override
    protected CloseFuture doCloseGracefully() {
        synchronized (buffer) {
            return builder().when(pending).build().close(false);
        }
    }

    @SuppressWarnings("synthetic-access")
    private void doRead(boolean resume) {
        IoReadFutureImpl future = null;
        int nbRead = 0;
        synchronized (buffer) {
            if (buffer.available() > 0) {
                if (resume) {
                }
                future = pending;
                pending = null;
                if (future != null) {
                    nbRead = future.buffer.putBuffer(buffer, false);
                    buffer.compact();
                }
            } else {
                if (!resume) {
                }
            }
        }
        if (nbRead > 0) {
            Channel channel = getChannel();
            try {
                Window wLocal = channel.getLocalWindow();
                wLocal.consumeAndCheck(nbRead);
            } catch (IOException e) {
                Session session = channel.getSession();
                session.exceptionCaught(e);
            }
            future.setValue(nbRead);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getChannel() + "]";
    }

    public static class IoReadFutureImpl extends DefaultVerifiableSshFuture<IoReadFuture> implements IoReadFuture {
        private final Buffer buffer;

        public IoReadFutureImpl(Buffer buffer) {
            super(null);
            this.buffer = buffer;
        }

        @Override
        public Buffer getBuffer() {
            return buffer;
        }

        @Override
        public IoReadFuture verify(long timeoutMillis) throws IOException {
            long startTime = System.nanoTime();
            Number result = verifyResult(Number.class, timeoutMillis);
            long endTime = System.nanoTime();
            return this;
        }

        @Override
        public int getRead() {
            Object v = getValue();
            if (v instanceof RuntimeException) {
                throw (RuntimeException) v;
            } else if (v instanceof Error) {
                throw (Error) v;
            } else if (v instanceof Throwable) {
                throw new RuntimeSshException("Error reading from channel.", (Throwable) v);
            } else if (v instanceof Number) {
                return ((Number) v).intValue();
            } else {
                throw new IllegalStateException("Unknown read value type: " + ((v == null) ? "null" : v.getClass().getName()));
            }
        }

        @Override
        public Throwable getException() {
            Object v = getValue();
            if (v instanceof Throwable) {
                return (Throwable) v;
            } else {
                return null;
            }
        }
    }
}

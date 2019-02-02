package org.xbib.io.sshd.common.io.nio2;

import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.RuntimeSshException;
import org.xbib.io.sshd.common.future.CloseFuture;
import org.xbib.io.sshd.common.io.IoHandler;
import org.xbib.io.sshd.common.io.IoSession;
import org.xbib.io.sshd.common.io.IoWriteFuture;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.Readable;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.closeable.AbstractCloseable;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class Nio2Session extends AbstractCloseable implements IoSession {

    public static final int DEFAULT_READBUF_SIZE = 8 * 1024;

    private static final AtomicLong SESSION_ID_GENERATOR = new AtomicLong(100L);

    private final long id = SESSION_ID_GENERATOR.incrementAndGet();
    private final org.xbib.io.sshd.common.io.nio2.Nio2Service service;
    private final IoHandler ioHandler;
    private final AsynchronousSocketChannel socketChannel;
    private final Map<Object, Object> attributes = new HashMap<>();
    private final SocketAddress localAddress;
    private final SocketAddress remoteAddress;
    private final FactoryManager manager;
    private final Queue<Nio2DefaultIoWriteFuture> writes = new LinkedTransferQueue<>();
    private final AtomicReference<Nio2DefaultIoWriteFuture> currentWrite = new AtomicReference<>();

    public Nio2Session(org.xbib.io.sshd.common.io.nio2.Nio2Service service, FactoryManager manager, IoHandler handler, AsynchronousSocketChannel socket) throws IOException {
        this.service = Objects.requireNonNull(service, "No service instance");
        this.manager = Objects.requireNonNull(manager, "No factory manager");
        this.ioHandler = Objects.requireNonNull(handler, "No IoHandler");
        this.socketChannel = Objects.requireNonNull(socket, "No socket channel");
        this.localAddress = socket.getLocalAddress();
        this.remoteAddress = socket.getRemoteAddress();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Object getAttribute(Object key) {
        return attributes.get(key);
    }

    @Override
    public Object setAttribute(Object key, Object value) {
        return attributes.put(key, value);
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    public AsynchronousSocketChannel getSocket() {
        return socketChannel;
    }

    public IoHandler getIoHandler() {
        return ioHandler;
    }

    public void suspend() {
        AsynchronousSocketChannel socket = getSocket();
        try {
            socket.shutdownInput();
        } catch (IOException e) {
        }

        try {
            socket.shutdownOutput();
        } catch (IOException e) {
        }
    }

    @Override
    public IoWriteFuture writePacket(Buffer buffer) {
        ByteBuffer buf = ByteBuffer.wrap(buffer.array(), buffer.rpos(), buffer.available());
        final Nio2DefaultIoWriteFuture future = new Nio2DefaultIoWriteFuture(getRemoteAddress(),null, buf);
        if (isClosing()) {
            Throwable exc = new ClosedChannelException();
            future.setException(exc);
            exceptionCaught(exc);
            return future;
        }
        writes.add(future);
        startWriting();
        return future;
    }

    protected void exceptionCaught(Throwable exc) {
        if (!closeFuture.isClosed()) {
            AsynchronousSocketChannel socket = getSocket();
            if (isClosing() || !socket.isOpen()) {
                close(true);
            } else {
                IoHandler handler = getIoHandler();
                try {
                    handler.exceptionCaught(this, exc);
                } catch (Throwable e) {
                    Throwable t = GenericUtils.peelException(e);
                    close(true);
                }
            }
        }
    }

    @Override
    protected CloseFuture doCloseGracefully() {
        Object closeId = toString();
        return builder().when(closeId, writes).run(closeId, () -> {
            try {
                AsynchronousSocketChannel socket = getSocket();
                socket.shutdownOutput();
            } catch (IOException e) {
            }
        }).build().close(false);
    }

    @Override
    protected void doCloseImmediately() {
        for (; ; ) {
            Nio2DefaultIoWriteFuture future = writes.poll();
            if (future != null) {
                future.setException(new ClosedChannelException());
            } else {
                break;
            }
        }

        AsynchronousSocketChannel socket = getSocket();
        try {
            socket.close();
        } catch (IOException e) {
        }

        service.sessionClosed(this);
        super.doCloseImmediately();

        IoHandler handler = getIoHandler();
        try {
            handler.sessionClosed(this);
        } catch (Throwable e) {
        }
    }

    @Override   // co-variant return
    public Nio2Service getService() {
        return service;
    }

    public void startReading() {
        startReading(manager.getIntProperty(FactoryManager.NIO2_READ_BUFFER_SIZE, DEFAULT_READBUF_SIZE));
    }

    public void startReading(int bufSize) {
        startReading(new byte[bufSize]);
    }

    public void startReading(byte[] buf) {
        startReading(buf, 0, buf.length);
    }

    public void startReading(byte[] buf, int offset, int len) {
        startReading(ByteBuffer.wrap(buf, offset, len));
    }

    public void startReading(ByteBuffer buffer) {
        doReadCycle(buffer, Readable.readable(buffer));
    }

    protected void doReadCycle(ByteBuffer buffer, Readable bufReader) {
        Nio2CompletionHandler<Integer, Object> completion =
                Objects.requireNonNull(createReadCycleCompletionHandler(buffer, bufReader), "No completion handler created");
        doReadCycle(buffer, completion);
    }

    protected Nio2CompletionHandler<Integer, Object> createReadCycleCompletionHandler(final ByteBuffer buffer, final Readable bufReader) {
        return new Nio2CompletionHandler<Integer, Object>() {
            @Override
            protected void onCompleted(Integer result, Object attachment) {
                handleReadCycleCompletion(buffer, bufReader, this, result, attachment);
            }

            @Override
            protected void onFailed(Throwable exc, Object attachment) {
                handleReadCycleFailure(buffer, bufReader, exc, attachment);
            }
        };
    }

    protected void handleReadCycleCompletion(
            ByteBuffer buffer, Readable bufReader, Nio2CompletionHandler<Integer, Object> completionHandler, Integer result, Object attachment) {
        try {
            if (result >= 0) {
                buffer.flip();

                IoHandler handler = getIoHandler();
                handler.messageReceived(this, bufReader);
                if (!closeFuture.isClosed()) {
                    // re-use reference for next iteration since we finished processing it
                    buffer.clear();
                    doReadCycle(buffer, completionHandler);
                } else {
                }
            } else {
                close(true);
            }
        } catch (Throwable exc) {
            completionHandler.failed(exc, attachment);
        }
    }

    protected void handleReadCycleFailure(ByteBuffer buffer, Readable bufReader, Throwable exc, Object attachment) {
        exceptionCaught(exc);
    }

    protected void doReadCycle(ByteBuffer buffer, Nio2CompletionHandler<Integer, Object> completion) {
        AsynchronousSocketChannel socket = getSocket();
        long readTimeout = manager.getLongProperty(FactoryManager.NIO2_READ_TIMEOUT, FactoryManager.DEFAULT_NIO2_READ_TIMEOUT);
        socket.read(buffer, readTimeout, TimeUnit.MILLISECONDS, null, completion);
    }

    protected void startWriting() {
        Nio2DefaultIoWriteFuture future = writes.peek();
        if (future != null) {
            if (currentWrite.compareAndSet(null, future)) {
                try {
                    AsynchronousSocketChannel socket = getSocket();
                    ByteBuffer buffer = future.getBuffer();
                    Nio2CompletionHandler<Integer, Object> handler =
                            Objects.requireNonNull(createWriteCycleCompletionHandler(future, socket, buffer),
                                    "No write cycle completion handler created");
                    doWriteCycle(buffer, handler);
                } catch (Throwable e) {
                    future.setWritten();
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    } else {
                        throw new RuntimeSshException(e);
                    }
                }
            }
        }
    }

    protected void doWriteCycle(ByteBuffer buffer, Nio2CompletionHandler<Integer, Object> completion) {
        AsynchronousSocketChannel socket = getSocket();
        long writeTimeout = manager.getLongProperty(FactoryManager.NIO2_MIN_WRITE_TIMEOUT, FactoryManager.DEFAULT_NIO2_MIN_WRITE_TIMEOUT);
        socket.write(buffer, writeTimeout, TimeUnit.MILLISECONDS, null, completion);
    }

    protected Nio2CompletionHandler<Integer, Object> createWriteCycleCompletionHandler(
            final Nio2DefaultIoWriteFuture future, final AsynchronousSocketChannel socket, final ByteBuffer buffer) {
        final int writeLen = buffer.remaining();
        return new Nio2CompletionHandler<Integer, Object>() {
            @Override
            protected void onCompleted(Integer result, Object attachment) {
                handleCompletedWriteCycle(future, socket, buffer, writeLen, this, result, attachment);
            }

            @Override
            protected void onFailed(Throwable exc, Object attachment) {
                handleWriteCycleFailure(future, socket, buffer, writeLen, exc, attachment);
            }
        };
    }

    protected void handleCompletedWriteCycle(
            Nio2DefaultIoWriteFuture future, AsynchronousSocketChannel socket, ByteBuffer buffer, int writeLen,
            Nio2CompletionHandler<Integer, Object> completionHandler, Integer result, Object attachment) {
        if (buffer.hasRemaining()) {
            try {
                socket.write(buffer, null, completionHandler);
            } catch (Throwable t) {
                future.setWritten();
                finishWrite(future);
            }
        } else {
            future.setWritten();
            finishWrite(future);
        }
    }

    protected void handleWriteCycleFailure(
            Nio2DefaultIoWriteFuture future, AsynchronousSocketChannel socket,
            ByteBuffer buffer, int writeLen, Throwable exc, Object attachment) {
        future.setException(exc);
        exceptionCaught(exc);
        finishWrite(future);
    }

    protected void finishWrite(Nio2DefaultIoWriteFuture future) {
        writes.remove(future);
        currentWrite.compareAndSet(future, null);
        startWriting();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[local=" + getLocalAddress() + ", remote=" + getRemoteAddress() + "]";
    }
}

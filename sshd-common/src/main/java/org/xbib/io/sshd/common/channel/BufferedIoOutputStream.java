package org.xbib.io.sshd.common.channel;

import org.xbib.io.sshd.common.Closeable;
import org.xbib.io.sshd.common.future.SshFutureListener;
import org.xbib.io.sshd.common.io.IoOutputStream;
import org.xbib.io.sshd.common.io.IoWriteFuture;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.closeable.AbstractInnerCloseable;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An {@link IoOutputStream} capable of queuing write requests.
 */
public class BufferedIoOutputStream extends AbstractInnerCloseable implements IoOutputStream {
    protected final IoOutputStream out;
    protected final Queue<IoWriteFutureImpl> writes = new ConcurrentLinkedQueue<>();
    protected final AtomicReference<IoWriteFutureImpl> currentWrite = new AtomicReference<>();

    public BufferedIoOutputStream(IoOutputStream out) {
        this.out = out;
    }

    @Override
    public IoWriteFuture write(Buffer buffer) {
        IoWriteFutureImpl future = new IoWriteFutureImpl(buffer);
        if (isClosing()) {
            future.setValue(new IOException("Closed"));
        } else {
            writes.add(future);
            startWriting();
        }
        return future;
    }

    protected void startWriting() {
        final IoWriteFutureImpl future = writes.peek();
        if (future != null) {
            if (currentWrite.compareAndSet(null, future)) {
                out.write(future.getBuffer()).addListener(new SshFutureListener<IoWriteFuture>() {
                    @Override
                    public void operationComplete(IoWriteFuture f) {
                        if (f.isWritten()) {
                            future.setValue(Boolean.TRUE);
                        } else {
                            future.setValue(f.getException());
                        }
                        finishWrite();
                    }

                    private void finishWrite() {
                        writes.remove(future);
                        currentWrite.compareAndSet(future, null);
                        startWriting();
                    }
                });
            }
        }
    }

    @Override
    protected Closeable getInnerCloseable() {
        return builder()
                .when(writes)
                .close(out)
                .build();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + out + "]";
    }
}

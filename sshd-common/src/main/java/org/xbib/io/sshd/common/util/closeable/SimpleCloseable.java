package org.xbib.io.sshd.common.util.closeable;

import org.xbib.io.sshd.common.future.CloseFuture;
import org.xbib.io.sshd.common.future.DefaultCloseFuture;
import org.xbib.io.sshd.common.future.SshFutureListener;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class SimpleCloseable extends IoBaseCloseable {

    protected final DefaultCloseFuture future;
    protected final AtomicBoolean closing;

    public SimpleCloseable(Object id, Object lock) {
        future = new DefaultCloseFuture(id, lock);
        closing = new AtomicBoolean();
    }

    @Override
    public boolean isClosed() {
        return future.isClosed();
    }

    @Override
    public boolean isClosing() {
        return closing.get();
    }

    @Override
    public void addCloseFutureListener(SshFutureListener<CloseFuture> listener) {
        future.addListener(listener);
    }

    @Override
    public void removeCloseFutureListener(SshFutureListener<CloseFuture> listener) {
        future.removeListener(listener);
    }

    @Override
    public CloseFuture close(boolean immediately) {
        if (closing.compareAndSet(false, true)) {
            doClose(immediately);
        }
        return future;
    }

    protected void doClose(boolean immediately) {
        future.setClosed();
    }
}

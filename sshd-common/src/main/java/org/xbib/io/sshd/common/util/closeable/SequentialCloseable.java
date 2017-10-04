package org.xbib.io.sshd.common.util.closeable;

import org.xbib.io.sshd.common.Closeable;
import org.xbib.io.sshd.common.future.CloseFuture;
import org.xbib.io.sshd.common.future.SshFutureListener;

import java.util.Collections;
import java.util.Iterator;

/**
 * Waits for a group of {@link Closeable}s to complete in the given order, then
 * signals the completion by setting the &quot;parent&quot; future as closed.
 */
public class SequentialCloseable extends SimpleCloseable {
    private final Iterable<? extends Closeable> closeables;

    public SequentialCloseable(Object lock, Iterable<? extends Closeable> closeables) {
        super(lock);
        this.closeables = (closeables == null) ? Collections.emptyList() : closeables;
    }

    @Override
    protected void doClose(boolean immediately) {
        Iterator<? extends Closeable> iterator = closeables.iterator();
        SshFutureListener<CloseFuture> listener = new SshFutureListener<CloseFuture>() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void operationComplete(CloseFuture previousFuture) {
                while (iterator.hasNext()) {
                    Closeable c = iterator.next();
                    if (c != null) {
                        CloseFuture nextFuture = c.close(immediately);
                        nextFuture.addListener(this);
                        return;
                    }
                }
                if (!iterator.hasNext()) {
                    future.setClosed();
                }
            }
        };
        listener.operationComplete(null);
    }
}

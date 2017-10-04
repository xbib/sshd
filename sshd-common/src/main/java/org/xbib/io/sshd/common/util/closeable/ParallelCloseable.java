package org.xbib.io.sshd.common.util.closeable;

import org.xbib.io.sshd.common.Closeable;
import org.xbib.io.sshd.common.future.CloseFuture;
import org.xbib.io.sshd.common.future.SshFutureListener;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Waits for a group of {@link Closeable}s to complete in any order, then
 * signals the completion by setting the &quot;parent&quot; future as closed.
 */
public class ParallelCloseable extends SimpleCloseable {

    private final Iterable<? extends Closeable> closeables;

    public ParallelCloseable(Object lock, Iterable<? extends Closeable> closeables) {
        super(lock);
        this.closeables = (closeables == null) ? Collections.emptyList() : closeables;
    }

    @Override
    protected void doClose(final boolean immediately) {
        final AtomicInteger count = new AtomicInteger(1);
        SshFutureListener<CloseFuture> listener = f -> {
            int pendingCount = count.decrementAndGet();
            if (pendingCount == 0) {
                future.setClosed();
            }
        };
        for (Closeable c : closeables) {
            if (c != null) {
                int pendingCount = count.incrementAndGet();
                c.close(immediately).addListener(listener);
            }
        }
        listener.operationComplete(null);
    }
}

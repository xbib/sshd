package org.xbib.io.sshd.common.util.closeable;

import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.future.DefaultSshFuture;
import org.xbib.io.sshd.common.future.SshFuture;
import org.xbib.io.sshd.common.future.SshFutureListener;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @param <T> Type of future
 */
public class FuturesCloseable<T extends SshFuture> extends SimpleCloseable {

    private final Iterable<? extends SshFuture<T>> futures;

    public FuturesCloseable(Object lock, Iterable<? extends SshFuture<T>> futures) {
        super(lock);
        this.futures = (futures == null) ? Collections.emptyList() : futures;
    }

    @Override
    protected void doClose(final boolean immediately) {
        if (immediately) {
            for (SshFuture<?> f : futures) {
                if (f instanceof DefaultSshFuture) {
                    ((DefaultSshFuture<?>) f).setValue(new SshException("Closed"));
                }
            }
            future.setClosed();
        } else {
            final AtomicInteger count = new AtomicInteger(1);
            SshFutureListener<T> listener = f -> {
                int pendingCount = count.decrementAndGet();
                if (pendingCount == 0) {
                    future.setClosed();
                }
            };
            for (SshFuture<T> f : futures) {
                if (f != null) {
                    int pendingCount = count.incrementAndGet();
                    f.addListener(listener);
                }
            }
            listener.operationComplete(null);
        }
    }
}

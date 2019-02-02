package org.xbib.io.sshd.common.util.closeable;

import org.xbib.io.sshd.common.Closeable;
import org.xbib.io.sshd.common.future.SshFuture;
import org.xbib.io.sshd.common.util.ObjectBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 *
 */
public final class Builder implements ObjectBuilder<Closeable> {
    private final Object lock;
    private final List<Closeable> closeables = new ArrayList<>();

    public Builder(Object lock) {
        this.lock = Objects.requireNonNull(lock, "No lock");
    }

    public Builder run(Object id, Runnable r) {
        return close(new SimpleCloseable(id, lock) {
            @Override
            protected void doClose(boolean immediately) {
                try {
                    r.run();
                } finally {
                    super.doClose(immediately);
                }
            }
        });
    }

    @SuppressWarnings("rawtypes")
    public <T extends SshFuture> Builder when(SshFuture<T> future) {
        if (future != null) {
            when(future.getId(), Collections.singleton(future));
        }
        return this;
    }

    @SuppressWarnings("rawtypes")
    @SafeVarargs
    public final <T extends SshFuture> Builder when(SshFuture<T>... futures) {
        return when(getClass().getSimpleName(), Arrays.asList(futures));
    }

    @SuppressWarnings("rawtypes")
    public <T extends SshFuture> Builder when(Object id, Iterable<? extends SshFuture<T>> futures) {
        return close(new FuturesCloseable<>(id, lock, futures));
    }

    public Builder sequential(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            close(closeable);
        }
        return this;
    }

    public Builder sequential(Object id, Iterable<Closeable> closeables) {
        return close(new SequentialCloseable(id, lock, closeables));
    }

    public Builder parallel(Closeable... closeables) {
        if (closeables.length == 1) {
            close(closeables[0]);
        } else if (closeables.length > 0) {
            parallel(getClass().getSimpleName(), Arrays.asList(closeables));
        }
        return this;
    }

    public Builder parallel(Object id, Iterable<? extends Closeable> closeables) {
        return close(new ParallelCloseable(id, lock, closeables));
    }

    public Builder close(Closeable c) {
        if (c != null) {
            closeables.add(c);
        }
        return this;
    }

    @Override
    public Closeable build() {
        if (closeables.isEmpty()) {
            return new SimpleCloseable(getClass().getSimpleName(), lock);
        } else if (closeables.size() == 1) {
            return closeables.get(0);
        } else {
            return new SequentialCloseable(getClass().getSimpleName(), lock, closeables);
        }
    }
}

package org.xbib.io.sshd.common.util.closeable;

import org.xbib.io.sshd.common.future.CloseFuture;
import org.xbib.io.sshd.common.future.DefaultCloseFuture;
import org.xbib.io.sshd.common.future.SshFuture;
import org.xbib.io.sshd.common.future.SshFutureListener;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides some default implementations.
 */
public abstract class AbstractCloseable extends IoBaseCloseable {

    /**
     * Lock object for this session state
     */
    protected final Object lock = new Object();
    /**
     * State of this object
     */
    protected final AtomicReference<State> state = new AtomicReference<>(State.Opened);
    /**
     * A future that will be set 'closed' when the object is actually closed
     */
    protected final CloseFuture closeFuture = new DefaultCloseFuture(lock);

    protected AbstractCloseable() {
        this("");
    }

    protected AbstractCloseable(String discriminator) {
        super(discriminator);
    }

    @Override
    public void addCloseFutureListener(SshFutureListener<CloseFuture> listener) {
        closeFuture.addListener(listener);
    }

    @Override
    public void removeCloseFutureListener(SshFutureListener<CloseFuture> listener) {
        closeFuture.removeListener(listener);
    }

    @Override
    public CloseFuture close(boolean immediately) {
        if (immediately) {
            if (state.compareAndSet(State.Opened, State.Immediate)
                    || state.compareAndSet(State.Graceful, State.Immediate)) {
                preClose();
                doCloseImmediately();
            } else {
            }
        } else {
            if (state.compareAndSet(State.Opened, State.Graceful)) {
                preClose();
                SshFuture<CloseFuture> grace = doCloseGracefully();
                if (grace != null) {
                    grace.addListener(future -> {
                        if (state.compareAndSet(State.Graceful, State.Immediate)) {
                            doCloseImmediately();
                        }
                    });
                } else {
                    if (state.compareAndSet(State.Graceful, State.Immediate)) {
                        doCloseImmediately();
                    }
                }
            } else {
            }
        }
        return closeFuture;
    }

    @Override
    public boolean isClosed() {
        return state.get() == State.Closed;
    }

    @Override
    public boolean isClosing() {
        return state.get() != State.Opened;
    }

    /**
     * preClose is guaranteed to be called before doCloseGracefully or doCloseImmediately.
     * When preClose() is called, isClosing() == true
     */
    protected void preClose() {
        // nothing
    }

    protected CloseFuture doCloseGracefully() {
        return null;
    }

    /**
     * doCloseImmediately is called once and only once
     * with state == Immediate
     * Overriding methods should always call the base implementation.
     * It may be called concurrently while preClose() or doCloseGracefully is executing
     */
    protected void doCloseImmediately() {
        closeFuture.setClosed();
        state.set(State.Closed);
    }

    protected org.xbib.io.sshd.common.util.closeable.Builder builder() {
        return new Builder(lock);
    }

    public enum State {
        Opened, Graceful, Immediate, Closed
    }
}
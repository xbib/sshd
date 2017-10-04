package org.xbib.io.sshd.common.future;

/**
 * A default implementation of {@link CloseFuture}.
 */
public class DefaultCloseFuture extends DefaultSshFuture<CloseFuture> implements CloseFuture {

    /**
     * Create a new instance
     *
     * @param lock A synchronization object for locking access - if {@code null}
     *             then synchronization occurs on {@code this} instance
     */
    public DefaultCloseFuture(Object lock) {
        super(lock);
    }

    @Override
    public boolean isClosed() {
        if (isDone()) {
            return (Boolean) getValue();
        } else {
            return false;
        }
    }

    @Override
    public void setClosed() {
        setValue(Boolean.TRUE);
    }
}

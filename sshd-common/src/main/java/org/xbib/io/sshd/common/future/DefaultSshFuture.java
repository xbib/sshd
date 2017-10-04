package org.xbib.io.sshd.common.future;

import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;

import java.io.InterruptedIOException;
import java.lang.reflect.Array;
import java.util.Objects;

/**
 * A default implementation of {@link org.xbib.io.sshd.common.future.SshFuture}.
 *
 * @param <T> Type of future
 */
public class DefaultSshFuture<T extends SshFuture> extends AbstractSshFuture<T> {
    /**
     * A lock used by the wait() method
     */
    private final Object lock;
    private Object listeners;
    private Object result;

    /**
     * Creates a new instance.
     *
     * @param lock A synchronization object for locking access - if {@code null}
     *             then synchronization occurs on {@code this} instance
     */
    public DefaultSshFuture(Object lock) {
        this.lock = lock != null ? lock : this;
    }

    @Override
    protected Object await0(long timeoutMillis, boolean interruptable) throws InterruptedIOException {
        ValidateUtils.checkTrue(timeoutMillis >= 0L, "Negative timeout N/A: %d", timeoutMillis);
        long startTime = System.currentTimeMillis();
        long curTime = startTime;
        long endTime = ((Long.MAX_VALUE - timeoutMillis) < curTime) ? Long.MAX_VALUE : (curTime + timeoutMillis);

        synchronized (lock) {
            if ((result != null) || (timeoutMillis <= 0)) {
                return result;
            }

            for (; ; ) {
                try {
                    lock.wait(endTime - curTime);
                } catch (InterruptedException e) {
                    if (interruptable) {
                        curTime = System.currentTimeMillis();
                        throw (InterruptedIOException) new InterruptedIOException("Interrupted after " + (curTime - startTime) + " msec.").initCause(e);
                    }
                }

                curTime = System.currentTimeMillis();
                if ((result != null) || (curTime >= endTime)) {
                    return result;
                }
            }
        }
    }

    @Override
    public boolean isDone() {
        synchronized (lock) {
            return result != null;
        }
    }

    public int getNumRegisteredListeners() {
        synchronized (lock) {
            if (listeners == null) {
                return 0;
            } else if (listeners instanceof SshFutureListener) {
                return 1;
            } else {
                int l = Array.getLength(listeners);
                int count = 0;
                for (int i = 0; i < l; i++) {
                    if (Array.get(listeners, i) != null) {
                        count++;
                    }
                }
                return count;
            }
        }
    }

    /**
     * @return The result of the asynchronous operation - or {@code null}
     * if none set.
     */
    public Object getValue() {
        synchronized (lock) {
            return (result == GenericUtils.NULL) ? null : result;
        }
    }

    /**
     * Sets the result of the asynchronous operation, and mark it as finished.
     *
     * @param newValue The operation result
     */
    public void setValue(Object newValue) {
        synchronized (lock) {
            // Allow only once.
            if (result != null) {
                return;
            }

            result = (newValue != null) ? newValue : GenericUtils.NULL;
            lock.notifyAll();
        }

        notifyListeners();
    }

    @Override
    public T addListener(SshFutureListener<T> listener) {
        Objects.requireNonNull(listener, "Missing listener argument");
        boolean notifyNow = false;
        synchronized (lock) {
            // if already have a result don't register the listener and invoke it directly
            if (result != null) {
                notifyNow = true;
            } else if (listeners == null) {
                listeners = listener;   // 1st listener ?
            } else if (listeners instanceof SshFutureListener) {
                listeners = new Object[]{listeners, listener};
            } else {    // increase array of registered listeners
                Object[] ol = (Object[]) listeners;
                int l = ol.length;
                Object[] nl = new Object[l + 1];
                System.arraycopy(ol, 0, nl, 0, l);
                nl[l] = listener;
                listeners = nl;
            }
        }

        if (notifyNow) {
            notifyListener(listener);
        }
        return asT();
    }

    @Override
    public T removeListener(SshFutureListener<T> listener) {
        Objects.requireNonNull(listener, "No listener provided");

        synchronized (lock) {
            if (result != null) {
                return asT();   // the train has already left the station...
            }

            if (listeners == null) {
                return asT();   // no registered instances anyway
            }

            if (listeners == listener) {
                listeners = null;   // the one and only
            } else if (!(listeners instanceof SshFutureListener)) {
                int l = Array.getLength(listeners);
                for (int i = 0; i < l; i++) {
                    if (Array.get(listeners, i) == listener) {
                        Array.set(listeners, i, null);
                        break;
                    }
                }
            }
        }

        return asT();
    }

    protected void notifyListeners() {
        /*
         * There won't be any visibility problem or concurrent modification
         * because result value is checked in both addListener and
         * removeListener calls under lock. If the result is already set then
         * both methods will not modify the internal listeners
         */
        if (listeners != null) {
            if (listeners instanceof SshFutureListener) {
                notifyListener(asListener(listeners));
            } else {
                int l = Array.getLength(listeners);
                for (int i = 0; i < l; i++) {
                    SshFutureListener<T> listener = asListener(Array.get(listeners, i));
                    if (listener != null) {
                        notifyListener(listener);
                    }
                }
            }
        }
    }

    public boolean isCanceled() {
        return getValue() == CANCELED;
    }

    public void cancel() {
        setValue(CANCELED);
    }
}

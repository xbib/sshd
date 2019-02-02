package org.xbib.io.sshd.common.future;

import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.StreamCorruptedException;

/**
 * @param <T> Type of future
 */
public abstract class AbstractSshFuture<T extends SshFuture> extends AbstractLoggingBean implements SshFuture<T> {
    /**
     * A default value to indicate the future has been canceled
     */
    protected static final Object CANCELED = new Object();

    private final Object id;

    protected AbstractSshFuture(Object id) {
        this.id = id;
    }

    @Override
    public Object getId() {
        return id;
    }

    @Override
    public boolean await(long timeoutMillis) throws IOException {
        return await0(timeoutMillis, true) != null;
    }

    @Override
    public boolean awaitUninterruptibly(long timeoutMillis) {
        try {
            return await0(timeoutMillis, false) != null;
        } catch (InterruptedIOException e) {
            throw new InternalError("Unexpected interrupted exception wile awaitUninterruptibly "
                    + timeoutMillis + " msec.: " + e.getMessage(), e);
        }
    }

    /**
     * Waits (interruptible) for the specified timeout (msec.) and then checks
     * the result:
     * <UL>
     * <LI>
     * If result is {@code null} then timeout is assumed to have expired - throw
     * an appropriate {@link IOException}
     * </LI>
     * <LI>
     * If the result is of the expected type, then cast and return it
     * </LI>
     * <LI>
     * If the result is an {@link IOException} then re-throw it
     * </LI>
     * <LI>
     * If the result is a {@link Throwable} then throw an {@link IOException}
     * whose cause is the original exception
     * </LI>
     * <LI>
     * Otherwise (should never happen), throw a {@link StreamCorruptedException}
     * with the name of the result type
     * </LI>
     * </UL>
     *
     * @param <R>          The generic result type
     * @param expectedType The expected result type
     * @param timeout      The timeout (millis) to wait for a result
     * @return The (never {@code null}) result
     * @throws IOException If failed to retrieve the expected result on time
     */
    protected <R> R verifyResult(Class<? extends R> expectedType, long timeout) throws IOException {
        Object value = await0(timeout, true);
        if (value == null) {
            throw new SshException("Failed to get operation result within specified timeout: " + timeout);
        }

        Class<?> actualType = value.getClass();
        if (expectedType.isAssignableFrom(actualType)) {
            return expectedType.cast(value);
        }

        if (Throwable.class.isAssignableFrom(actualType)) {
            Throwable t = GenericUtils.peelException((Throwable) value);
            if (t != value) {
                value = t;
                actualType = value.getClass();
            }

            if (IOException.class.isAssignableFrom(actualType)) {
                throw (IOException) value;
            }

            throw new SshException("Failed (" + t.getClass().getSimpleName() + ") to execute: " + t.getMessage(), GenericUtils.resolveExceptionCause(t));
        } else {    // what else can it be ????
            throw new StreamCorruptedException("Unknown result type: " + actualType.getName());
        }
    }

    /**
     * Wait for the Future to be ready. If the requested delay is 0 or
     * negative, this method returns immediately.
     *
     * @param timeoutMillis The delay we will wait for the Future to be ready
     * @param interruptable Tells if the wait can be interrupted or not.
     *                      If {@code true} and the thread is interrupted then an {@link InterruptedIOException}
     *                      is thrown.
     * @return The non-{@code null} result object if the Future is ready,
     * {@code null} if the timeout expired and no result was received
     * @throws InterruptedIOException If the thread has been interrupted
     *                                when it's not allowed.
     */
    protected abstract Object await0(long timeoutMillis, boolean interruptable) throws InterruptedIOException;

    @SuppressWarnings("unchecked")
    protected SshFutureListener<T> asListener(Object o) {
        return (SshFutureListener<T>) o;
    }

    protected void notifyListener(SshFutureListener<T> l) {
        try {
            l.operationComplete(asT());
        } catch (Throwable t) {
        }
    }

    @SuppressWarnings("unchecked")
    protected T asT() {
        return (T) this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[id=" + getId() + "]";
    }
}

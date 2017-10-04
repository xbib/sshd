package org.xbib.io.sshd.common.future;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Represents an asynchronous operation whose successful result can be
 * verified somehow. The contract guarantees that if the {@code verifyXXX}
 * method returns without an exception then the operation was completed
 * <U>successfully</U>
 *
 * @param <T> Type of verification result
 */
@FunctionalInterface
public interface VerifiableFuture<T> {
    /**
     * Wait {@link Long#MAX_VALUE} msec. and verify that the operation was successful
     *
     * @return The (same) future instance
     * @throws IOException If failed to verify successfully on time
     * @see #verify(long)
     */
    default T verify() throws IOException {
        return verify(Long.MAX_VALUE);
    }

    /**
     * Wait and verify that the operation was successful
     *
     * @param timeout The number of time units to wait
     * @param unit    The wait {@link TimeUnit}
     * @return The (same) future instance
     * @throws IOException If failed to verify successfully on time
     * @see #verify(long)
     */
    default T verify(long timeout, TimeUnit unit) throws IOException {
        return verify(unit.toMillis(timeout));
    }

    /**
     * Wait and verify that the operation was successful
     *
     * @param timeoutMillis Wait timeout in milliseconds
     * @return The (same) future instance
     * @throws IOException If failed to verify successfully on time
     */
    T verify(long timeoutMillis) throws IOException;
}

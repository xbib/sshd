package org.xbib.io.sshd.common.channel;

import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.PropertyResolver;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.BufferUtils;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.net.SocketTimeoutException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * A Window for a given channel.
 * Windows are used to not overflow the client or server when sending datas.
 * Both clients and servers have a local and remote window and won't send
 * anymore data until the window has been expanded.
 */
public class Window extends AbstractLoggingBean implements java.nio.channels.Channel, ChannelHolder {
    /**
     * Default {@link Predicate} used to test if space became available
     */
    public static final Predicate<Window> SPACE_AVAILABLE_PREDICATE = input -> {
        // NOTE: we do not call "getSize()" on purpose in order to avoid the lock
        return input.sizeHolder.get() > 0;
    };

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicLong sizeHolder = new AtomicLong(0L);
    private final AbstractChannel channelInstance;
    private final Object lock;
    private final String suffix;

    private long maxSize;   // actually uint32
    private long packetSize;   // actually uint32

    public Window(AbstractChannel channel, Object lock, boolean client, boolean local) {
        this.channelInstance = Objects.requireNonNull(channel, "No channel provided");
        this.lock = (lock != null) ? lock : this;
        this.suffix = (client ? "client" : "server") + "/" + (local ? "local" : "remote");
    }

    @Override   // co-variant return
    public AbstractChannel getChannel() {
        return channelInstance;
    }

    public long getSize() {
        synchronized (lock) {
            return sizeHolder.get();
        }
    }

    public long getMaxSize() {
        return maxSize;
    }

    public long getPacketSize() {
        return packetSize;
    }

    public void init(PropertyResolver resolver) {
        init(resolver.getLongProperty(FactoryManager.WINDOW_SIZE, FactoryManager.DEFAULT_WINDOW_SIZE),
                resolver.getLongProperty(FactoryManager.MAX_PACKET_SIZE, FactoryManager.DEFAULT_MAX_PACKET_SIZE),
                resolver);
    }

    public void init(long size, long packetSize, PropertyResolver resolver) {
        BufferUtils.validateUint32Value(size, "Illegal initial size: %d");
        BufferUtils.validateUint32Value(packetSize, "Illegal packet size: %d");
        ValidateUtils.checkTrue(packetSize > 0L, "Packet size must be positive: %d", packetSize);
        long limitPacketSize = resolver.getLongProperty(FactoryManager.LIMIT_PACKET_SIZE, FactoryManager.DEFAULT_LIMIT_PACKET_SIZE);
        if (packetSize > limitPacketSize) {
            throw new IllegalArgumentException("Requested packet size (" + packetSize + ") exceeds max. allowed: " + limitPacketSize);
        }

        synchronized (lock) {
            this.maxSize = size;
            this.packetSize = packetSize;
            updateSize(size);
        }

        if (initialized.getAndSet(true)) {
        }

    }

    public void expand(int window) {
        ValidateUtils.checkTrue(window >= 0, "Negative window size: %d", window);
        checkInitialized("expand");

        long expandedSize;
        synchronized (lock) {
            /*
             * See RFC-4254 section 5.2:
             *
             *      "Implementations MUST correctly handle window sizes
             *      of up to 2^32 - 1 bytes.  The window MUST NOT be increased above
             *      2^32 - 1 bytes.
             */
            expandedSize = sizeHolder.get() + window;
            if (expandedSize > BufferUtils.MAX_UINT32_VALUE) {
                updateSize(BufferUtils.MAX_UINT32_VALUE);
            } else {
                updateSize(expandedSize);
            }
        }

        if (expandedSize > Integer.MAX_VALUE) {
        }
    }

    public void consume(long len) {
        BufferUtils.validateUint32Value(len, "Invalid consumption length: %d");
        checkInitialized("consume");

        long remainLen;
        synchronized (lock) {
            remainLen = sizeHolder.get() - len;
            if (remainLen >= 0L) {
                updateSize(remainLen);
            }
        }

        if (remainLen < 0L) {
            throw new IllegalStateException("consume(" + this + ") required length (" + len + ") above available: " + (remainLen + len));
        }

    }

    public void consumeAndCheck(long len) throws IOException {
        synchronized (lock) {
            try {
                consume(len);
                check(maxSize);
            } catch (RuntimeException e) {
                throw new StreamCorruptedException("consumeAndCheck(" + this + ")"
                        + " failed (" + e.getClass().getSimpleName() + ")"
                        + " to consume " + len + " bytes"
                        + ": " + e.getMessage());
            }
        }
    }

    public void check(long maxFree) throws IOException {
        BufferUtils.validateUint32Value(maxFree, "Invalid check size: %d");
        checkInitialized("check");

        long adjustSize = -1L;
        AbstractChannel channel = getChannel();
        synchronized (lock) {
            // TODO make the adjust factor configurable via FactoryManager property
            long size = sizeHolder.get();
            if (size < (maxFree / 2)) {
                adjustSize = maxFree - size;
                channel.sendWindowAdjust(adjustSize);
                updateSize(maxFree);
            }
        }

        if (adjustSize >= 0L) {
        }
    }

    /**
     * Waits for enough data to become available to consume the specified size
     *
     * @param len         Size of data to consume
     * @param maxWaitTime ax. time (millis) to wait for enough data to become available
     * @throws InterruptedException   If interrupted while waiting
     * @throws WindowClosedException  If window closed while waiting
     * @throws SocketTimeoutException If timeout expired before enough data became available
     * @see #waitForCondition(Predicate, long)
     * @see #consume(long)
     */
    public void waitAndConsume(long len, long maxWaitTime) throws InterruptedException, WindowClosedException, SocketTimeoutException {
        BufferUtils.validateUint32Value(len, "Invalid wait consume length: %d", len);
        checkInitialized("waitAndConsume");

        synchronized (lock) {
            waitForCondition(input -> {
                // NOTE: we do not call "getSize()" on purpose in order to avoid the lock
                return input.sizeHolder.get() >= len;
            }, maxWaitTime);

            consume(len);
        }
    }

    /**
     * Waits until some data becomes available or timeout expires
     *
     * @param maxWaitTime Max. time (millis) to wait for space to become available
     * @return Amount of available data - always positive
     * @throws InterruptedException   If interrupted while waiting
     * @throws WindowClosedException  If window closed while waiting
     * @throws SocketTimeoutException If timeout expired before space became available
     * @see #waitForCondition(Predicate, long)
     */
    public long waitForSpace(long maxWaitTime) throws InterruptedException, WindowClosedException, SocketTimeoutException {
        checkInitialized("waitForSpace");

        long available;
        synchronized (lock) {
            waitForCondition(SPACE_AVAILABLE_PREDICATE, maxWaitTime);
            available = sizeHolder.get();
        }

        return available;
    }

    /**
     * Waits up to a specified amount of time for a condition to be satisfied and
     * signaled via the lock. <B>Note:</B> assumes that lock is acquired when this
     * method is called.
     *
     * @param predicate   The {@link Predicate} to check if the condition has been
     *                    satisfied - the argument to the predicate is {@code this} reference
     * @param maxWaitTime Max. time (millis) to wait for the condition to be satisfied
     * @throws WindowClosedException  If window closed while waiting
     * @throws InterruptedException   If interrupted while waiting
     * @throws SocketTimeoutException If timeout expired before condition was satisfied
     * @see #isOpen()
     */
    protected void waitForCondition(Predicate<? super Window> predicate, long maxWaitTime)
            throws WindowClosedException, InterruptedException, SocketTimeoutException {
        Objects.requireNonNull(predicate, "No condition");
        ValidateUtils.checkTrue(maxWaitTime > 0, "Non-positive max. wait time: %d", maxWaitTime);

        long maxWaitNanos = TimeUnit.MILLISECONDS.toNanos(maxWaitTime);
        long remWaitNanos = maxWaitNanos;
        // The loop takes care of spurious wakeups
        while (isOpen() && (remWaitNanos > 0L)) {
            if (predicate.test(this)) {
                return;
            }

            long curWaitMillis = TimeUnit.NANOSECONDS.toMillis(remWaitNanos);
            long nanoWaitStart = System.nanoTime();
            if (curWaitMillis > 0L) {
                lock.wait(curWaitMillis);
            } else {    // only nanoseconds remaining
                lock.wait(0L, (int) remWaitNanos);
            }
            long nanoWaitEnd = System.nanoTime();
            long nanoWaitDuration = nanoWaitEnd - nanoWaitStart;
            remWaitNanos -= nanoWaitDuration;
        }

        if (!isOpen()) {
            throw new WindowClosedException(toString());
        }

        throw new SocketTimeoutException("waitForCondition(" + this + ") timeout exceeded: " + maxWaitTime);
    }

    protected void updateSize(long size) {
        BufferUtils.validateUint32Value(size, "Invalid updated size: %d", size);
        this.sizeHolder.set(size);
        lock.notifyAll();
    }

    protected void checkInitialized(String location) {
        if (!initialized.get()) {
            throw new IllegalStateException(location + " - window not initialized: " + this);
        }
    }

    @Override
    public boolean isOpen() {
        return !closed.get();
    }

    @Override
    public void close() throws IOException {
        if (!closed.getAndSet(true)) {
        }

        // just in case someone is still waiting
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + suffix + "](" + String.valueOf(getChannel()) + ")";
    }
}

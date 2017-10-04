package org.xbib.io.sshd.common.io.nio2;

import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.io.AbstractIoServiceFactoryFactory;
import org.xbib.io.sshd.common.io.IoServiceFactory;

import java.nio.channels.AsynchronousChannel;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 */
public class Nio2ServiceFactoryFactory extends AbstractIoServiceFactoryFactory {

    public Nio2ServiceFactoryFactory() {
        this(null, true);
    }

    /**
     * @param executors      The {@link ExecutorService} to use for spawning threads.
     *                       If {@code null} then an internal service is allocated - in which case it
     *                       is automatically shutdown regardless of the value of the <tt>shutdownOnExit</tt>
     *                       parameter value
     * @param shutdownOnExit If {@code true} then the {@link ExecutorService#shutdownNow()}
     *                       will be called (unless it is an internally allocated service which is always
     *                       closed)
     */
    public Nio2ServiceFactoryFactory(ExecutorService executors, boolean shutdownOnExit) {
        super(executors, shutdownOnExit);
        // Make sure NIO2 is available
        Objects.requireNonNull(AsynchronousChannel.class, "Missing NIO2 class");
    }

    @Override
    public IoServiceFactory create(FactoryManager manager) {
        return new Nio2ServiceFactory(manager, getExecutorService(), isShutdownOnExit());
    }
}

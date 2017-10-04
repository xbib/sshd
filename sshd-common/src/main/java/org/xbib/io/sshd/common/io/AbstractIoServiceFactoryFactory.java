package org.xbib.io.sshd.common.io;

import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;
import org.xbib.io.sshd.common.util.threads.ExecutorServiceConfigurer;

import java.util.concurrent.ExecutorService;

/**
 *
 */
public abstract class AbstractIoServiceFactoryFactory
        extends AbstractLoggingBean
        implements IoServiceFactoryFactory, ExecutorServiceConfigurer {

    private ExecutorService executorService;
    private boolean shutdownExecutor;

    /**
     * @param executors      The {@link ExecutorService} to use for spawning threads.
     *                       If {@code null} then an internal service is allocated - in which case it
     *                       is automatically shutdown regardless of the value of the <tt>shutdownOnExit</tt>
     *                       parameter value
     * @param shutdownOnExit If {@code true} then the {@link ExecutorService#shutdownNow()}
     *                       will be called (unless it is an internally allocated service which is always
     *                       closed)
     */
    protected AbstractIoServiceFactoryFactory(ExecutorService executors, boolean shutdownOnExit) {
        executorService = executors;
        shutdownExecutor = shutdownOnExit;
    }

    @Override
    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public void setExecutorService(ExecutorService service) {
        executorService = service;

    }

    @Override
    public boolean isShutdownOnExit() {
        return shutdownExecutor;
    }

    @Override
    public void setShutdownOnExit(boolean shutdown) {
        shutdownExecutor = shutdown;
    }

}

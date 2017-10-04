package org.xbib.io.sshd.common.io;

import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.FactoryManagerHolder;
import org.xbib.io.sshd.common.util.closeable.AbstractCloseable;
import org.xbib.io.sshd.common.util.threads.ExecutorServiceCarrier;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public abstract class AbstractIoServiceFactory
        extends AbstractCloseable
        implements IoServiceFactory, FactoryManagerHolder, ExecutorServiceCarrier {

    private final FactoryManager manager;
    private final ExecutorService executor;
    private final boolean shutdownExecutor;

    protected AbstractIoServiceFactory(FactoryManager factoryManager, ExecutorService executorService, boolean shutdownOnExit) {
        manager = factoryManager;
        executor = executorService;
        shutdownExecutor = shutdownOnExit;
    }

    public static int getNioWorkers(FactoryManager manager) {
        int nb = manager.getIntProperty(FactoryManager.NIO_WORKERS, FactoryManager.DEFAULT_NIO_WORKERS);
        if (nb > 0) {
            return nb;
        } else {
            return FactoryManager.DEFAULT_NIO_WORKERS;
        }
    }

    @Override
    public final FactoryManager getFactoryManager() {
        return manager;
    }

    @Override
    public final ExecutorService getExecutorService() {
        return executor;
    }

    @Override
    public final boolean isShutdownOnExit() {
        return shutdownExecutor;
    }

    @Override
    protected void doCloseImmediately() {
        try {
            ExecutorService service = getExecutorService();
            if ((service != null) && isShutdownOnExit() && (!service.isShutdown())) {
                service.shutdownNow();
                if (service.awaitTermination(5, TimeUnit.SECONDS)) {
                } else {
                }
            }
        } catch (Exception e) {
        } finally {
            super.doCloseImmediately();
        }
    }
}

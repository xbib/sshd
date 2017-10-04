package org.xbib.io.sshd.common.util.threads;

import java.util.concurrent.ExecutorService;

/**
 *
 */
public interface ExecutorServiceCarrier {
    /**
     * @return The {@link ExecutorService} to use
     */
    ExecutorService getExecutorService();

    /**
     * @return If {@code true} then the {@link ExecutorService#shutdownNow()}
     * will be called (unless it is an internally allocated service which is always
     * closed)
     */
    boolean isShutdownOnExit();
}

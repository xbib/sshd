package org.xbib.io.sshd.common.util.threads;

import java.util.concurrent.ExecutorService;

/**
 *
 */
public interface ExecutorServiceConfigurer extends ExecutorServiceCarrier {
    void setExecutorService(ExecutorService service);

    void setShutdownOnExit(boolean shutdown);
}

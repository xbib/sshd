package org.xbib.io.sshd.common.io.nio2;

import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.RuntimeSshException;
import org.xbib.io.sshd.common.io.AbstractIoServiceFactory;
import org.xbib.io.sshd.common.io.IoAcceptor;
import org.xbib.io.sshd.common.io.IoConnector;
import org.xbib.io.sshd.common.io.IoHandler;
import org.xbib.io.sshd.common.util.threads.ThreadUtils;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class Nio2ServiceFactory extends AbstractIoServiceFactory {

    private final AsynchronousChannelGroup group;

    public Nio2ServiceFactory(FactoryManager factoryManager, ExecutorService service, boolean shutdownOnExit) {
        super(factoryManager,
                service == null ? ThreadUtils.newFixedThreadPool(factoryManager.toString() + "-nio2", getNioWorkers(factoryManager)) : service,
                service == null || shutdownOnExit);
        try {
            group = AsynchronousChannelGroup.withThreadPool(ThreadUtils.protectExecutorServiceShutdown(getExecutorService(), isShutdownOnExit()));
        } catch (IOException e) {
            throw new RuntimeSshException(e);
        }
    }

    @Override
    public IoConnector createConnector(IoHandler handler) {
        return new Nio2Connector(getFactoryManager(), handler, group);
    }

    @Override
    public IoAcceptor createAcceptor(IoHandler handler) {
        return new Nio2Acceptor(getFactoryManager(), handler, group);
    }

    @Override
    protected void doCloseImmediately() {
        try {
            if (!group.isShutdown()) {
                group.shutdownNow();
                // if we protect the executor then the await will fail since we didn't really shut it down...
                if (isShutdownOnExit()) {
                    if (group.awaitTermination(5, TimeUnit.SECONDS)) {
                    } else {
                    }
                }
            }
        } catch (Exception e) {
        } finally {
            super.doCloseImmediately();
        }
    }
}

package org.apache.sshd.common.forward;

import org.apache.sshd.common.channel.Channel;
import org.apache.sshd.common.channel.ChannelFactory;
import org.apache.sshd.common.util.threads.ExecutorServiceCarrier;

import java.util.concurrent.ExecutorService;

/**
 *
 */
public abstract class TcpipFactory implements ChannelFactory, ExecutorServiceCarrier {

    private final WrappedForwardingFilter.Type type;

    protected TcpipFactory(WrappedForwardingFilter.Type type) {
        this.type = type;
    }

    public final WrappedForwardingFilter.Type getType() {
        return type;
    }

    @Override
    public final String getName() {
        return type.getName();
    }

    @Override
    public ExecutorService getExecutorService() {
        return null;
    }

    @Override
    public boolean isShutdownOnExit() {
        return false;
    }

    @Override
    public abstract Channel create();
}
package org.xbib.io.sshd.common.forward;

import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.channel.ChannelFactory;
import org.xbib.io.sshd.common.util.threads.ExecutorServiceCarrier;

import java.util.concurrent.ExecutorService;

/**
 *
 */
public abstract class TcpipFactory implements ChannelFactory, ExecutorServiceCarrier {

    private final ForwardingFilter.Type type;

    protected TcpipFactory(ForwardingFilter.Type type) {
        this.type = type;
    }

    public final ForwardingFilter.Type getType() {
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
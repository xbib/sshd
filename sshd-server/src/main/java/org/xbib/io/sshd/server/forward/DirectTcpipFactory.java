package org.xbib.io.sshd.server.forward;

import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.forward.ForwardingFilter;
import org.xbib.io.sshd.common.forward.TcpipFactory;

/**
 *
 */
public class DirectTcpipFactory extends TcpipFactory {

    public static final DirectTcpipFactory INSTANCE = new DirectTcpipFactory();

    public DirectTcpipFactory() {
        super(ForwardingFilter.Type.Direct);
    }

    @Override
    public Channel create() {
        TcpipServerChannel channel = new TcpipServerChannel(getType());
        channel.setExecutorService(getExecutorService());
        channel.setShutdownOnExit(isShutdownOnExit());
        return channel;
    }
}

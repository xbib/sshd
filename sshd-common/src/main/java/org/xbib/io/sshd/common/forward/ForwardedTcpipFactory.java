package org.xbib.io.sshd.common.forward;

import org.xbib.io.sshd.common.channel.Channel;

/**
 *
 */
public class ForwardedTcpipFactory extends TcpipFactory {

    public static final ForwardedTcpipFactory INSTANCE = new ForwardedTcpipFactory();

    public ForwardedTcpipFactory() {
        super(ForwardingFilter.Type.Forwarded);
    }

    @Override
    public Channel create() {
        return null;
    }
}

package org.xbib.io.sshd.server.channel;

import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.channel.ChannelFactory;

/**
 *
 */
public class ChannelSessionFactory implements ChannelFactory {
    public static final ChannelSessionFactory INSTANCE = new ChannelSessionFactory();

    public ChannelSessionFactory() {
        super();
    }

    @Override
    public String getName() {
        return "session";
    }

    @Override
    public Channel create() {
        return new ChannelSession();
    }
}
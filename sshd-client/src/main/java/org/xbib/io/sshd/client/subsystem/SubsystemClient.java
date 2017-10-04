package org.xbib.io.sshd.client.subsystem;

import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.client.session.ClientSessionHolder;
import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.channel.ChannelHolder;
import org.xbib.io.sshd.common.channel.ClientChannel;
import org.xbib.io.sshd.common.session.SessionHolder;

import java.nio.channels.Channel;

/**
 *
 */
public interface SubsystemClient
        extends SessionHolder<ClientSession>,
        ClientSessionHolder,
        NamedResource,
        ChannelHolder,
        Channel {
    /**
     * @return The underlying {@link ClientChannel} used
     */
    ClientChannel getClientChannel();
}

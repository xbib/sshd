package org.xbib.io.sshd.server.agent.local;

import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.channel.ChannelFactory;
import org.xbib.io.sshd.common.util.ValidateUtils;

/**
 *
 */
public class ChannelAgentForwardingFactory implements ChannelFactory {
    public static final ChannelAgentForwardingFactory OPENSSH = new ChannelAgentForwardingFactory("auth-agent@openssh.com");
    // see https://tools.ietf.org/html/draft-ietf-secsh-agent-02
    public static final ChannelAgentForwardingFactory IETF = new ChannelAgentForwardingFactory("auth-agent");

    private final String name;

    public ChannelAgentForwardingFactory(String name) {
        this.name = ValidateUtils.checkNotNullAndNotEmpty(name, "No channel factory name specified");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Channel create() {
        return new ChannelAgentForwarding();
    }
}
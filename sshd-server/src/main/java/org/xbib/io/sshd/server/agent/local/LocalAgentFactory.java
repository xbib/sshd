package org.xbib.io.sshd.server.agent.local;

import org.xbib.io.sshd.common.agent.SshAgent;
import org.xbib.io.sshd.common.agent.SshAgentFactory;
import org.xbib.io.sshd.common.agent.SshAgentServer;
import org.xbib.io.sshd.common.agent.AgentDelegate;
import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.session.ConnectionService;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class LocalAgentFactory implements SshAgentFactory {
    public static final List<NamedFactory<Channel>> DEFAULT_FORWARDING_CHANNELS =
            Collections.unmodifiableList(
                    Arrays.<NamedFactory<Channel>>asList(ChannelAgentForwardingFactory.OPENSSH, ChannelAgentForwardingFactory.IETF));

    private final SshAgent agent;

    public LocalAgentFactory() {
        this.agent = new AgentImpl();
    }

    public LocalAgentFactory(SshAgent agent) {
        this.agent = agent;
    }

    public SshAgent getAgent() {
        return agent;
    }

    @Override
    public List<NamedFactory<Channel>> getChannelForwardingFactories(FactoryManager manager) {
        return DEFAULT_FORWARDING_CHANNELS;
    }

    @Override
    public SshAgent createClient(FactoryManager manager) throws IOException {
        return new AgentDelegate(agent);
    }

    @Override
    public SshAgentServer createServer(ConnectionService service) throws IOException {
        return null;
    }
}

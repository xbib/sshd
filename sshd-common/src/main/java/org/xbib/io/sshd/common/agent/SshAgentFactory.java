package org.xbib.io.sshd.common.agent;

import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.session.ConnectionService;

import java.io.IOException;
import java.util.List;

/**
 * The <code>SshAgentFactory</code> is used to communicate with an SshAgent.
 */
public interface SshAgentFactory {
    /**
     * Value that can be set in order to control the type of authentication
     * channel being requested when forwarding a PTY session. If not defined
     * then {@link #DEFAULT_PROXY_AUTH_CHANNEL_TYPE} is used
     */
    String PROXY_AUTH_CHANNEL_TYPE = "ssh-agent-factory-proxy-auth-channel-type";
    // see also https://tools.ietf.org/html/draft-ietf-secsh-agent-02
    String DEFAULT_PROXY_AUTH_CHANNEL_TYPE = "auth-agent-req@openssh.com";

    /**
     * The channels are requested by the ssh server when forwarding a client request.
     * The channel will receive agent requests and need to forward them to the agent,
     * either local or through another proxy.
     *
     * @param manager The {@link FactoryManager} through which the request is made
     * @return The (named) channel factories used to create channels on the client side
     */
    List<NamedFactory<Channel>> getChannelForwardingFactories(FactoryManager manager);

    /**
     * Create an SshAgent that can be used on the client side by the authentication
     * process to send possible keys.
     *
     * @param manager The {@link FactoryManager} instance
     * @return The {@link SshAgent} instance
     * @throws IOException If failed to create the client
     */
    SshAgent createClient(FactoryManager manager) throws IOException;

    /**
     * Create the server side that will be used by other SSH clients.
     * It will usually create a channel that will forward the requests
     * to the original client.
     *
     * @param service The {@link ConnectionService} to use
     * @return The {@link SshAgentServer} instance
     * @throws IOException If failed to create the server
     */
    SshAgentServer createServer(ConnectionService service) throws IOException;
}

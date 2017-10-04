package org.xbib.io.sshd.common.session;

import org.xbib.io.sshd.common.agent.AgentForwardSupport;
import org.xbib.io.sshd.common.Service;
import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.forward.PortForwardingEventListenerManager;
import org.xbib.io.sshd.common.forward.PortForwardingEventListenerManagerHolder;
import org.xbib.io.sshd.common.forward.TcpipForwarder;
import org.xbib.io.sshd.common.x11.X11ForwardSupport;

import java.io.IOException;

/**
 * Interface implementing ssh-connection service.
 */
public interface ConnectionService extends Service, PortForwardingEventListenerManager, PortForwardingEventListenerManagerHolder {
    /**
     * Register a newly created channel with a new unique identifier
     *
     * @param channel The {@link Channel} to register
     * @return The assigned id of this channel
     * @throws IOException If failed to initialize and register the channel
     */
    int registerChannel(Channel channel) throws IOException;

    /**
     * Remove this channel from the list of managed channels
     *
     * @param channel The {@link Channel} instance
     */
    void unregisterChannel(Channel channel);

    /**
     * Retrieve the tcpip forwarder
     *
     * @return The {@link TcpipForwarder}
     */
    TcpipForwarder getTcpipForwarder();

    // TODO: remove from interface, it's server side only
    AgentForwardSupport getAgentForwardSupport();

    // TODO: remove from interface, it's server side only
    X11ForwardSupport getX11ForwardSupport();

    boolean isAllowMoreSessions();

    void setAllowMoreSessions(boolean allow);
}

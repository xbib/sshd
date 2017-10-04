package org.xbib.io.sshd.common.forward;

import org.xbib.io.sshd.common.session.ConnectionService;

/**
 * A factory for creating forwarder objects for client port forwarding.
 */
@FunctionalInterface
public interface TcpipForwarderFactory {

    /**
     * Creates the forwarder to be used for TCP/IP port forwards for this session.
     *
     * @param service the {@link ConnectionService} the connections are forwarded through
     * @return the {@link org.xbib.io.sshd.common.forward.TcpipForwarder} that will listen for connections and set up forwarding
     */
    TcpipForwarder create(ConnectionService service);
}

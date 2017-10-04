package org.xbib.io.sshd.common.agent;

import org.xbib.io.sshd.common.Closeable;

import java.io.IOException;

/**
 * The server side fake agent, acting as an agent, but actually forwarding
 * the requests to the auth channel on the client side.
 */
public interface AgentForwardSupport extends Closeable {
    /**
     * Initializes the agent forwarding if not already done so - i.e.,
     * can be called more than once - only first successful call counts,
     * the rest will return the identifier of the previously initialized
     * agent.
     *
     * @return The agent ID
     * @throws IOException If failed to initialize
     */
    String initialize() throws IOException;
}

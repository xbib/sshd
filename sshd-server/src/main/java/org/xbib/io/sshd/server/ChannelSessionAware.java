package org.xbib.io.sshd.server;

import org.xbib.io.sshd.server.channel.ChannelSession;

/**
 * {@link Command} can implement this optional interface
 * to receive a reference to {@link ChannelSession}.
 */
@FunctionalInterface
public interface ChannelSessionAware {
    /**
     * Receives the channel in which the command is being executed.
     *
     * @param session never null
     */
    void setChannelSession(ChannelSession session);
}

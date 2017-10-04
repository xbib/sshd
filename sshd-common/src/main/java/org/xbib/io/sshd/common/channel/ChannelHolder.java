package org.xbib.io.sshd.common.channel;

/**
 *
 */
@FunctionalInterface
public interface ChannelHolder {
    /**
     * @return The associated {@link Channel} instance
     */
    Channel getChannel();
}

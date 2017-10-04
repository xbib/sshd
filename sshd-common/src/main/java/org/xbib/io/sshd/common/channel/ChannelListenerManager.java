package org.xbib.io.sshd.common.channel;


/**
 *
 */
public interface ChannelListenerManager {
    /**
     * Add a channel listener
     *
     * @param listener The {@link ChannelListener} to add - not {@code null}
     */
    void addChannelListener(ChannelListener listener);

    /**
     * Remove a channel listener
     *
     * @param listener The {@link ChannelListener} to remove
     */
    void removeChannelListener(ChannelListener listener);

    /**
     * @return A (never {@code null} proxy {@link ChannelListener} that represents
     * all the currently registered listeners. Any method invocation on the proxy
     * is replicated to the currently registered listeners
     */
    ChannelListener getChannelListenerProxy();

}

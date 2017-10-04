package org.xbib.io.sshd.common.forward;

/**
 * Marker interface for classes that allow to add/remove port forwarding
 * listeners. <B>Note:</B> if adding/removing listeners while tunnels are
 * being established and/or torn down there are no guarantees as to the order
 * of the calls to the recently added/removed listener's methods in the interim.
 * The correct order is guaranteed only as of the <U>next</U> tunnel after
 * the listener has been added/removed.
 */
public interface PortForwardingEventListenerManager {
    /**
     * Add a port forwarding listener
     *
     * @param listener The {@link PortForwardingEventListener} to add - never {@code null}
     */
    void addPortForwardingEventListener(PortForwardingEventListener listener);

    /**
     * Remove a port forwarding listener
     *
     * @param listener The {@link PortForwardingEventListener} to remove - ignored if {@code null}
     */
    void removePortForwardingEventListener(PortForwardingEventListener listener);

    /**
     * @return A proxy listener representing all the currently registered listener
     * through this manager
     */
    PortForwardingEventListener getPortForwardingEventListenerProxy();
}

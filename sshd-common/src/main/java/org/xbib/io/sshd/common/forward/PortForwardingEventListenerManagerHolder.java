package org.xbib.io.sshd.common.forward;

import java.util.Collection;

/**
 *
 */
public interface PortForwardingEventListenerManagerHolder {
    /**
     * @return The currently registered managers. <B>Note:</B> it is highly
     * recommended that implementors return either an un-modifiable collection
     * or a <U>copy</U> of the current one. Callers, should avoid modifying
     * the retrieved value.
     */
    Collection<PortForwardingEventListenerManager> getRegisteredManagers();

    boolean addPortForwardingEventListenerManager(PortForwardingEventListenerManager manager);

    boolean removePortForwardingEventListenerManager(PortForwardingEventListenerManager manager);
}

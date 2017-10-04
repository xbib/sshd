package org.xbib.io.sshd.common.channel;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Various events used by {@link ClientChannel#waitFor(java.util.Collection, long)}
 */
public enum ClientChannelEvent {
    /**
     * Timeout while waiting for other events - <B>Note:</B> meaningful only
     * as a member of the <U>returned</U> events
     **/
    TIMEOUT,
    /**
     * Channel has been marked as closed
     **/
    CLOSED,
    /**
     * Received STDOUT (a.k.a. channel) data
     **/
    STDOUT_DATA,
    /**
     * Received STDERR (a.k.a. extended) data
     **/
    STDERR_DATA,
    /**
     * Received EOF signal from remote peer
     **/
    EOF,
    /**
     * Received exit status from remote peer
     *
     * @see ClientChannel#getExitStatus()
     **/
    EXIT_STATUS,
    /**
     * Received exit signal from remote peer
     *
     * @see ClientChannel#getExitSignal()
     */
    EXIT_SIGNAL,
    /**
     * Channel has been successfully opened
     */
    OPENED;

    public static final Set<ClientChannelEvent> VALUES =
            Collections.unmodifiableSet(EnumSet.allOf(ClientChannelEvent.class));
}

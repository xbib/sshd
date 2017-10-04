package org.xbib.io.sshd.common.util;

/**
 * Notify about the occurrence of an event
 *
 * @param <E> type of event being notified
 */
@FunctionalInterface
public interface EventNotifier<E> {
    /**
     * @param event The event
     * @throws Exception If failed to process the event notification
     */
    void notifyEvent(E event) throws Exception;
}

package org.xbib.io.sshd.server.subsystem.sftp;

/**
 *
 */
public interface SftpEventListenerManager {
    /**
     * @return An instance representing <U>all</U> the currently
     * registered listeners. Any method invocation is <U>replicated</U>
     * to the actually registered listeners
     */
    SftpEventListener getSftpEventListenerProxy();

    /**
     * Register a listener instance
     *
     * @param listener The {@link SftpEventListener} instance to add - never {@code null}
     * @return {@code true} if listener is a previously un-registered one
     */
    boolean addSftpEventListener(SftpEventListener listener);

    /**
     * Remove a listener instance
     *
     * @param listener The {@link SftpEventListener} instance to remove - never {@code null}
     * @return {@code true} if listener is a (removed) registered one
     */
    boolean removeSftpEventListener(SftpEventListener listener);
}

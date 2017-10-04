package org.xbib.io.sshd.common.session;

/**
 *
 */
public interface ReservedSessionMessagesManager {
    /**
     * @return The currently registered {@link ReservedSessionMessagesHandler} - may be {@code null}
     */
    ReservedSessionMessagesHandler getReservedSessionMessagesHandler();

    /**
     * @param handler The {@link ReservedSessionMessagesHandler} to use - may be {@code null}
     */
    void setReservedSessionMessagesHandler(ReservedSessionMessagesHandler handler);
}

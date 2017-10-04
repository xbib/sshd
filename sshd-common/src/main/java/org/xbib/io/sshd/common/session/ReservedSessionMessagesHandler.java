package org.xbib.io.sshd.common.session;

import org.xbib.io.sshd.common.util.SshdEventListener;
import org.xbib.io.sshd.common.util.buffer.Buffer;

/**
 * Provides a way to listen and handle the {@code SSH_MSG_IGNORE} and
 * {@code SSH_MSG_DEBUG} messages that are received by a session.
 */
public interface ReservedSessionMessagesHandler extends SshdEventListener {
    /**
     * Invoked when an {@code SSH_MSG_IGNORE} packet is received
     *
     * @param session The {@code Session} through which the message was received
     * @param buffer  The {@code Buffer} containing the data
     * @throws Exception If failed to handle the message
     * @see <A HREF="https://tools.ietf.org/html/rfc4253#section-11.2">RFC 4253 - section 11.2</A>
     */
    default void handleIgnoreMessage(org.xbib.io.sshd.common.session.Session session, Buffer buffer) throws Exception {
        // ignored
    }

    /**
     * Invoked when an {@code SSH_MSG_DEBUG} packet is received
     *
     * @param session The {@code Session} through which the message was received
     * @param buffer  The {@code Buffer} containing the data
     * @throws Exception If failed to handle the message
     * @see <A HREF="https://tools.ietf.org/html/rfc4253#section-11.3">RFC 4253 - section 11.3</A>
     */
    default void handleDebugMessage(org.xbib.io.sshd.common.session.Session session, Buffer buffer) throws Exception {
        // ignored
    }

    /**
     * Invoked when an {@code SSH_MSG_UNIMPLEMENTED} packet is received
     *
     * @param session The {@code Session} through which the message was received
     * @param buffer  The {@code Buffer} containing the data
     * @throws Exception If failed to handle the message
     * @see <A HREF="https://tools.ietf.org/html/rfc4253#section-11.4">RFC 4253 - section 11.4</A>
     */
    default void handleUnimplementedMessage(Session session, Buffer buffer) throws Exception {
        // ignored
    }
}

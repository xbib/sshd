package org.xbib.io.sshd.common.channel;

import org.xbib.io.sshd.common.AttributeStore;
import org.xbib.io.sshd.common.Closeable;
import org.xbib.io.sshd.common.PropertyResolver;
import org.xbib.io.sshd.common.future.OpenFuture;
import org.xbib.io.sshd.common.session.ConnectionService;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Represents a channel opened over an SSH session - holds information that is
 * common both to server and client channels.
 */
public interface Channel
        extends ChannelListenerManager,
        PropertyResolver,
        AttributeStore,
        Closeable {
    // Known types of channels
    String CHANNEL_EXEC = "exec";
    String CHANNEL_SHELL = "shell";
    String CHANNEL_SUBSYSTEM = "subsystem";

    /**
     * @return Local channel identifier
     */
    int getId();

    /**
     * @return Remote channel identifier
     */
    int getRecipient();

    /**
     * @return The channel's underlying {@link Session}
     */
    Session getSession();

    org.xbib.io.sshd.common.channel.Window getLocalWindow();

    Window getRemoteWindow();

    List<RequestHandler<Channel>> getRequestHandlers();

    void addRequestHandler(RequestHandler<Channel> handler);

    default void addRequestHandlers(Collection<? extends RequestHandler<Channel>> handlers) {
        GenericUtils.forEach(handlers, this::addRequestHandler);
    }

    void removeRequestHandler(RequestHandler<Channel> handler);

    default void removeRequestHandlers(Collection<? extends RequestHandler<Channel>> handlers) {
        GenericUtils.forEach(handlers, this::removeRequestHandler);
    }

    /**
     * Invoked when <code>SSH_MSG_CHANNEL_CLOSE</code> received
     *
     * @throws IOException If failed to handle the message
     */
    void handleClose() throws IOException;

    /**
     * Invoked when <code>SSH_MSG_CHANNEL_WINDOW_ADJUST</code> received
     *
     * @param buffer The rest of the message data {@link Buffer} after
     *               decoding the channel identifiers
     * @throws IOException If failed to handle the message
     */
    void handleWindowAdjust(Buffer buffer) throws IOException;

    /**
     * Invoked when <code>SSH_MSG_CHANNEL_REQUEST</code> received
     *
     * @param buffer The rest of the message data {@link Buffer} after
     *               decoding the channel identifiers
     * @throws IOException If failed to handle the message
     */
    void handleRequest(Buffer buffer) throws IOException;

    /**
     * Invoked when <code>SSH_MSG_CHANNEL_DATA</code> received
     *
     * @param buffer The rest of the message data {@link Buffer} after
     *               decoding the channel identifiers
     * @throws IOException If failed to handle the message
     */
    void handleData(Buffer buffer) throws IOException;

    /**
     * Invoked when <code>SSH_MSG_CHANNEL_EXTENDED_DATA</code> received
     *
     * @param buffer The rest of the message data {@link Buffer} after
     *               decoding the channel identifiers
     * @throws IOException If failed to handle the message
     */
    void handleExtendedData(Buffer buffer) throws IOException;

    /**
     * Invoked when <code>SSH_MSG_CHANNEL_EOF</code> received
     *
     * @throws IOException If failed to handle the message
     */
    void handleEof() throws IOException;

    /**
     * Invoked when <code>SSH_MSG_CHANNEL_SUCCESS</code> received
     *
     * @throws IOException If failed to handle the message
     */
    void handleSuccess() throws IOException;

    /**
     * Invoked when <code>SSH_MSG_CHANNEL_FAILURE</code> received
     *
     * @throws IOException If failed to handle the message
     */
    void handleFailure() throws IOException;

    /**
     * Invoked when the local channel is initial created
     *
     * @param service The {@link ConnectionService} through which the channel is initialized
     * @param session The {@link Session} associated with the channel
     * @param id      The locally assigned channel identifier
     * @throws IOException If failed to process the initialization
     */
    void init(ConnectionService service, Session session, int id) throws IOException;

    /**
     * @return {@code true} if call to {@link #init(ConnectionService, Session, int)} was
     * successfully completed
     */
    boolean isInitialized();

    /**
     * @return {@code true} if the peer signaled that it will not send any
     * more data
     * @see <A HREF="https://www.ietf.org/rfc/rfc4254.txt">RFC 4254 - section 5.3 - SSH_MSG_CHANNEL_EOE</A>
     */
    boolean isEofSignalled();

    /**
     * For a server channel, this method will actually open the channel
     *
     * @param recipient  Recipient identifier
     * @param rwSize     Read/Write window size ({@code uint32})
     * @param packetSize Preferred maximum packet size ({@code uint32})
     * @param buffer     Incoming {@link Buffer} that triggered the call.
     *                   <B>Note:</B> the buffer's read position is exactly
     *                   <U>after</U> the information that read to this call
     *                   was decoded
     * @return An {@link OpenFuture} for the channel open request
     */
    OpenFuture open(int recipient, long rwSize, long packetSize, Buffer buffer);

    /**
     * For a client channel, this method will be called internally by the
     * session when the confirmation has been received.
     *
     * @param recipient  Recipient identifier
     * @param rwSize     Read/Write window size ({@code uint32})
     * @param packetSize Preferred maximum packet size ({@code uint32})
     * @param buffer     Incoming {@link Buffer} that triggered the call.
     *                   <B>Note:</B> the buffer's read position is exactly
     *                   <U>after</U> the information that read to this call
     *                   was decoded
     * @throws IOException If failed to handle the success
     */
    void handleOpenSuccess(int recipient, long rwSize, long packetSize, Buffer buffer) throws IOException;

    /**
     * For a client channel, this method will be called internally by the
     * session when the server has rejected this channel opening.
     *
     * @param buffer Incoming {@link Buffer} that triggered the call.
     *               <B>Note:</B> the buffer's read position is exactly
     *               <U>after</U> the information that read to this call
     *               was decoded
     * @throws IOException If failed to handle the success
     */
    void handleOpenFailure(Buffer buffer) throws IOException;
}

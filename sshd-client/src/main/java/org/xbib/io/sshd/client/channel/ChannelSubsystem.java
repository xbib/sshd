package org.xbib.io.sshd.client.channel;

import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.io.IOException;
import java.util.Date;

/**
 * Client channel to run a subsystem.
 */
public class ChannelSubsystem extends ChannelSession {
    /**
     * Configure whether reply for the &quot;subsystem&quot; request is required
     *
     * @see #DEFAULT_REQUEST_SUBSYSTEM_REPLY
     */
    public static final String REQUEST_SUBSYSTEM_REPLY = "channel-subsystem-want-reply";

    /**
     * <p>
     * Default value for {@link #REQUEST_SUBSYSTEM_REPLY} - according to
     * <A HREF="https://tools.ietf.org/html/rfc4254#section-6.5">RFC4254 section 6.5:</A>
     * </P>
     * <p>
     * It is RECOMMENDED that the reply to these messages be requested and checked.
     * </P>
     */
    public static final boolean DEFAULT_REQUEST_SUBSYSTEM_REPLY = true;

    private final String subsystem;

    /**
     * @param subsystem The subsystem name for the channel - never {@code null} or empty
     */
    public ChannelSubsystem(String subsystem) {
        this.subsystem = ValidateUtils.checkNotNullAndNotEmpty(subsystem, "Subsystem may not be null/empty");
    }

    /**
     * The subsystem name
     *
     * @return The subsystem name for the channel - never {@code null} or empty
     */
    public final String getSubsystem() {
        return subsystem;
    }

    @Override
    protected void doOpen() throws IOException {
        String systemName = getSubsystem();
        Session session = getSession();
        boolean wantReply = this.getBooleanProperty(REQUEST_SUBSYSTEM_REPLY, DEFAULT_REQUEST_SUBSYSTEM_REPLY);
        Buffer buffer = session.createBuffer(SshConstants.SSH_MSG_CHANNEL_REQUEST,
                Channel.CHANNEL_SUBSYSTEM.length() + systemName.length() + Integer.SIZE);
        buffer.putInt(getRecipient());
        buffer.putString(Channel.CHANNEL_SUBSYSTEM);
        buffer.putBoolean(wantReply);
        buffer.putString(systemName);
        addPendingRequest(Channel.CHANNEL_SUBSYSTEM, wantReply);
        writePacket(buffer);

        super.doOpen();
    }

    @Override
    public void handleSuccess() throws IOException {
        String systemName = getSubsystem();
        Date pending = removePendingRequest(Channel.CHANNEL_SUBSYSTEM);
    }

    @Override
    public void handleFailure() throws IOException {
        String systemName = getSubsystem();
        Date pending = removePendingRequest(Channel.CHANNEL_SUBSYSTEM);
        if (pending != null) {
            close(true);
        }
    }

    public void onClose(final Runnable run) {
        closeFuture.addListener(future -> run.run());
    }

    @Override
    public String toString() {
        return super.toString() + "[" + getSubsystem() + "]";
    }
}

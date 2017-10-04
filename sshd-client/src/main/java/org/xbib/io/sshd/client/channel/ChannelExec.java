package org.xbib.io.sshd.client.channel;

import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.io.IOException;
import java.util.Date;

/**
 * Client channel to run a remote command.
 */
public class ChannelExec extends PtyCapableChannelSession {
    /**
     * Configure whether reply for the &quot;exec&quot; request is required
     *
     * @see #DEFAULT_REQUEST_EXEC_REPLY
     */
    public static final String REQUEST_EXEC_REPLY = "channel-exec-want-reply";
    public static final boolean DEFAULT_REQUEST_EXEC_REPLY = false;

    private final String command;

    public ChannelExec(String command) {
        super(false);
        this.command = ValidateUtils.checkNotNullAndNotEmpty(command, "Command may not be null/empty");
    }

    @Override
    protected void doOpen() throws IOException {
        doOpenPty();

        Session session = getSession();
        boolean wantReply = this.getBooleanProperty(REQUEST_EXEC_REPLY, DEFAULT_REQUEST_EXEC_REPLY);
        Buffer buffer = session.createBuffer(SshConstants.SSH_MSG_CHANNEL_REQUEST, command.length() + Integer.SIZE);
        buffer.putInt(getRecipient());
        buffer.putString(Channel.CHANNEL_EXEC);
        buffer.putBoolean(wantReply);
        buffer.putString(command);
        addPendingRequest(Channel.CHANNEL_EXEC, wantReply);
        writePacket(buffer);

        super.doOpen();
    }

    @Override
    public void handleSuccess() throws IOException {
        Date pending = removePendingRequest(Channel.CHANNEL_EXEC);
    }

    @Override
    public void handleFailure() throws IOException {
        Date pending = removePendingRequest(Channel.CHANNEL_EXEC);
        if (pending != null) {
            close(true);
        }
    }
}

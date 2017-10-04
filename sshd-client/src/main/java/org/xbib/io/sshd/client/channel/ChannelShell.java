package org.xbib.io.sshd.client.channel;

import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.io.IOException;
import java.util.Date;

/**
 * Client channel to open a remote shell.
 */
public class ChannelShell extends PtyCapableChannelSession {
    /**
     * Configure whether reply for the &quot;shell&quot; request is required
     *
     * @see #DEFAULT_REQUEST_SHELL_REPLY
     */
    public static final String REQUEST_SHELL_REPLY = "channel-shell-want-reply";
    public static final boolean DEFAULT_REQUEST_SHELL_REPLY = false;

    public ChannelShell() {
        super(true);
    }

    @Override
    protected void doOpen() throws IOException {
        doOpenPty();

        Session session = getSession();
        boolean wantReply = this.getBooleanProperty(REQUEST_SHELL_REPLY, DEFAULT_REQUEST_SHELL_REPLY);
        Buffer buffer = session.createBuffer(SshConstants.SSH_MSG_CHANNEL_REQUEST, Integer.SIZE);
        buffer.putInt(getRecipient());
        buffer.putString(Channel.CHANNEL_SHELL);
        buffer.putBoolean(wantReply);
        addPendingRequest(Channel.CHANNEL_SHELL, wantReply);
        writePacket(buffer);

        super.doOpen();
    }

    @Override
    public void handleSuccess() throws IOException {
        Date pending = removePendingRequest(Channel.CHANNEL_SHELL);
    }

    @Override
    public void handleFailure() throws IOException {
        Date pending = removePendingRequest(Channel.CHANNEL_SHELL);
        if (pending != null) {
            close(true);
        }
    }
}

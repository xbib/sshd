package org.xbib.io.sshd.common.channel;

import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.future.DefaultOpenFuture;
import org.xbib.io.sshd.common.future.OpenFuture;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.net.SshdSocketAddress;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *
 */
public class ChannelDirectTcpip extends AbstractClientChannel {

    private final SshdSocketAddress local;
    private final SshdSocketAddress remote;
    private ChannelPipedOutputStream pipe;

    public ChannelDirectTcpip(SshdSocketAddress local, SshdSocketAddress remote) {
        super("direct-tcpip");
        if (local == null) {
            try {
                local = new SshdSocketAddress(InetAddress.getLocalHost().getHostName(), 0);
            } catch (UnknownHostException e) {
                throw new IllegalStateException("Unable to retrieve local host name");
            }
        }
        if (remote == null) {
            throw new IllegalArgumentException("Remote address must not be null");
        }
        this.local = local;
        this.remote = remote;
    }

    @Override
    public synchronized OpenFuture open() throws IOException {
        if (closeFuture.isClosed()) {
            throw new SshException("Session has been closed");
        }

        openFuture = new DefaultOpenFuture(lock);

        Session session = getSession();
        String remoteName = remote.getHostName();
        String localName = local.getHostName();
        Window wLocal = getLocalWindow();
        Buffer buffer = session.createBuffer(SshConstants.SSH_MSG_CHANNEL_OPEN,
                type.length() + remoteName.length() + localName.length() + Long.SIZE);
        buffer.putString(type);
        buffer.putInt(getId());
        buffer.putInt(wLocal.getSize());
        buffer.putInt(wLocal.getPacketSize());
        buffer.putString(remoteName);
        buffer.putInt(remote.getPort());
        buffer.putString(localName);
        buffer.putInt(local.getPort());
        writePacket(buffer);
        return openFuture;
    }

    @Override
    protected void doOpen() throws IOException {
        if (streaming == Streaming.Async) {
            asyncIn = new ChannelAsyncOutputStream(this, SshConstants.SSH_MSG_CHANNEL_DATA);
            asyncOut = new ChannelAsyncInputStream(this);
        } else {
            out = new ChannelOutputStream(this, getRemoteWindow(), SshConstants.SSH_MSG_CHANNEL_DATA, true);
            invertedIn = out;
            ChannelPipedInputStream pis = new ChannelPipedInputStream(this, getLocalWindow());
            pipe = new ChannelPipedOutputStream(pis);
            in = pis;
            invertedOut = in;
        }
    }

    @Override
    protected void doWriteData(byte[] data, int off, long len) throws IOException {
        ValidateUtils.checkTrue(len <= Integer.MAX_VALUE, "Data length exceeds int boundaries: %d", len);
        pipe.write(data, off, (int) len);
        pipe.flush();

        Window wLocal = getLocalWindow();
        wLocal.consumeAndCheck(len);
    }
}

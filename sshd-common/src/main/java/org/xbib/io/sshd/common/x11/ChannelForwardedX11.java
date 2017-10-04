package org.xbib.io.sshd.common.x11;

import org.xbib.io.sshd.common.Closeable;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.channel.AbstractClientChannel;
import org.xbib.io.sshd.common.channel.ChannelOutputStream;
import org.xbib.io.sshd.common.channel.Window;
import org.xbib.io.sshd.common.future.DefaultOpenFuture;
import org.xbib.io.sshd.common.future.OpenFuture;
import org.xbib.io.sshd.common.io.IoSession;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 *
 */
public class ChannelForwardedX11 extends AbstractClientChannel {
    private final IoSession serverSession;

    public ChannelForwardedX11(IoSession serverSession) {
        super("x11");
        this.serverSession = serverSession;
    }

    @Override
    public synchronized OpenFuture open() throws IOException {
        InetSocketAddress remote = (InetSocketAddress) serverSession.getRemoteAddress();
        if (closeFuture.isClosed()) {
            throw new SshException("Session has been closed");
        }
        openFuture = new DefaultOpenFuture(lock);

        Session session = getSession();

        InetAddress remoteAddress = remote.getAddress();
        String remoteHost = remoteAddress.getHostAddress();
        Window wLocal = getLocalWindow();
        Buffer buffer = session.createBuffer(SshConstants.SSH_MSG_CHANNEL_OPEN,
                remoteHost.length() + type.length() + Integer.SIZE);
        buffer.putString(type);
        buffer.putInt(getId());
        buffer.putInt(wLocal.getSize());
        buffer.putInt(wLocal.getPacketSize());
        buffer.putString(remoteHost);
        buffer.putInt(remote.getPort());
        writePacket(buffer);
        return openFuture;
    }

    @Override
    protected synchronized void doOpen() throws IOException {
        if (Streaming.Async.equals(streaming)) {
            throw new IllegalArgumentException("Asynchronous streaming isn't supported yet on this channel");
        }
        out = new ChannelOutputStream(this, getRemoteWindow(), SshConstants.SSH_MSG_CHANNEL_DATA, true);
        invertedIn = out;
    }

    @Override
    protected Closeable getInnerCloseable() {
        return builder().sequential(serverSession, super.getInnerCloseable()).build();
    }

    @Override
    protected synchronized void doWriteData(byte[] data, int off, long len) throws IOException {
        ValidateUtils.checkTrue(len <= Integer.MAX_VALUE, "Data length exceeds int boundaries: %d", len);
        Window wLocal = getLocalWindow();
        wLocal.consumeAndCheck(len);
        // use a clone in case data buffer is re-used
        serverSession.write(ByteArrayBuffer.getCompactClone(data, off, (int) len));
    }

    @Override
    public void handleEof() throws IOException {
        super.handleEof();
        serverSession.close(false);
    }
}

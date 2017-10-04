package org.xbib.io.sshd.common.forward;

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
import org.xbib.io.sshd.common.util.net.SshdSocketAddress;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 *
 */
public class TcpipClientChannel extends AbstractClientChannel {

    private final Type typeEnum;
    private final IoSession serverSession;
    private final SshdSocketAddress remote;
    public TcpipClientChannel(Type type, IoSession serverSession, SshdSocketAddress remote) {
        super(type == Type.Direct ? "direct-tcpip" : "forwarded-tcpip");
        this.typeEnum = type;
        this.serverSession = serverSession;
        this.remote = remote;
    }

    public OpenFuture getOpenFuture() {
        return openFuture;
    }

    @Override
    public synchronized OpenFuture open() throws IOException {
        final InetSocketAddress src;
        final InetSocketAddress dst;
        switch (typeEnum) {
            case Direct:
                src = (InetSocketAddress) serverSession.getRemoteAddress();
                dst = this.remote.toInetSocketAddress();
                break;
            case Forwarded:
                src = (InetSocketAddress) serverSession.getRemoteAddress();
                dst = (InetSocketAddress) serverSession.getLocalAddress();
                break;
            default:
                throw new SshException("Unknown client channel type: " + typeEnum);
        }
        if (closeFuture.isClosed()) {
            throw new SshException("Session has been closed");
        }
        openFuture = new DefaultOpenFuture(lock);

        Session session = getSession();
        InetAddress srcAddress = src.getAddress();
        String srcHost = srcAddress.getHostAddress();
        InetAddress dstAddress = dst.getAddress();
        String dstHost = dstAddress.getHostAddress();
        Window wLocal = getLocalWindow();
        Buffer buffer = session.createBuffer(SshConstants.SSH_MSG_CHANNEL_OPEN,
                type.length() + srcHost.length() + dstHost.length() + Long.SIZE);
        buffer.putString(type);
        buffer.putInt(getId());
        buffer.putInt(wLocal.getSize());
        buffer.putInt(wLocal.getPacketSize());
        buffer.putString(dstHost);
        buffer.putInt(dst.getPort());
        buffer.putString(srcHost);
        buffer.putInt(src.getPort());
        writePacket(buffer);
        return openFuture;
    }

    @Override
    protected synchronized void doOpen() throws IOException {
        if (streaming == Streaming.Async) {
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
        // Make sure we copy the data as the incoming buffer may be reused
        Buffer buf = ByteArrayBuffer.getCompactClone(data, off, (int) len);
        Window wLocal = getLocalWindow();
        wLocal.consumeAndCheck(len);
        serverSession.write(buf);
    }

    @Override
    protected void doWriteExtendedData(byte[] data, int off, long len) throws IOException {
        throw new UnsupportedOperationException(type + "Tcpip channel does not support extended data");
    }

    /**
     * Type of channel being created
     */
    public enum Type {
        Direct,
        Forwarded
    }
}

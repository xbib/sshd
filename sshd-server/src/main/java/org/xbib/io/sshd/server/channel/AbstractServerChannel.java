package org.xbib.io.sshd.server.channel;

import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.channel.AbstractChannel;
import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.channel.RequestHandler;
import org.xbib.io.sshd.common.channel.Window;
import org.xbib.io.sshd.common.future.DefaultOpenFuture;
import org.xbib.io.sshd.common.future.OpenFuture;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.server.session.ServerSession;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public abstract class AbstractServerChannel extends AbstractChannel implements ServerChannel {

    protected final AtomicBoolean exitStatusSent = new AtomicBoolean(false);

    protected AbstractServerChannel() {
        this(Collections.emptyList());
    }

    protected AbstractServerChannel(Collection<? extends RequestHandler<Channel>> handlers) {
        this("", handlers);
    }

    protected AbstractServerChannel(String discriminator, Collection<? extends RequestHandler<Channel>> handlers) {
        super(discriminator, false, handlers);
    }

    @Override
    public ServerSession getServerSession() {
        return (ServerSession) getSession();
    }

    @Override
    public OpenFuture open(int recipient, long rwSize, long packetSize, Buffer buffer) {
        setRecipient(recipient);

        Session s = getSession();
        FactoryManager manager = Objects.requireNonNull(s.getFactoryManager(), "No factory manager");
        Window wRemote = getRemoteWindow();
        wRemote.init(rwSize, packetSize, manager);
        configureWindow();
        return doInit(buffer);
    }

    @Override
    public void handleOpenSuccess(int recipient, long rwSize, long packetSize, Buffer buffer) throws IOException {
        throw new UnsupportedOperationException("handleOpenSuccess(" + recipient + "," + rwSize + "," + packetSize + ") N/A");
    }

    @Override
    public void handleOpenFailure(Buffer buffer) {
        throw new UnsupportedOperationException("handleOpenFailure() N/A");
    }

    protected OpenFuture doInit(Buffer buffer) {
        OpenFuture f = new DefaultOpenFuture(this);
        String changeEvent = "doInit";
        try {
            signalChannelOpenSuccess();
            f.setOpened();
        } catch (Throwable t) {
            Throwable e = GenericUtils.peelException(t);
            changeEvent = e.getClass().getSimpleName();
            signalChannelOpenFailure(e);
            f.setException(e);
        } finally {
            notifyStateChanged(changeEvent);
        }

        return f;
    }

    protected void sendExitStatus(int v) throws IOException {
        if (exitStatusSent.getAndSet(true)) {
            notifyStateChanged("exit-status");   // just in case
            return;
        }

        Session session = getSession();
        Buffer buffer = session.createBuffer(SshConstants.SSH_MSG_CHANNEL_REQUEST, Long.SIZE);
        buffer.putInt(getRecipient());
        buffer.putString("exit-status");
        buffer.putBoolean(false);   // want-reply - must be FALSE - see https://tools.ietf.org/html/rfc4254 section 6.10
        buffer.putInt(v);
        writePacket(buffer);
        notifyStateChanged("exit-status");
    }
}

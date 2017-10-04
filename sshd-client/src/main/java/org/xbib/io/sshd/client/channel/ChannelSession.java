package org.xbib.io.sshd.client.channel;

import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.channel.AbstractClientChannel;
import org.xbib.io.sshd.common.channel.ChannelAsyncInputStream;
import org.xbib.io.sshd.common.channel.ChannelAsyncOutputStream;
import org.xbib.io.sshd.common.channel.ChannelOutputStream;
import org.xbib.io.sshd.common.channel.ChannelPipedInputStream;
import org.xbib.io.sshd.common.channel.ChannelPipedOutputStream;
import org.xbib.io.sshd.common.channel.RequestHandler;
import org.xbib.io.sshd.common.channel.Window;
import org.xbib.io.sshd.common.future.CloseFuture;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.threads.ThreadUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 *
 */
public class ChannelSession extends AbstractClientChannel {

    private ExecutorService pumperService;
    private Future<?> pumper;
    private boolean shutdownPumper;

    public ChannelSession() {
        super("session");
    }

    @Override
    protected void doOpen() throws IOException {
        if (Streaming.Async.equals(streaming)) {
            asyncIn = new ChannelAsyncOutputStream(this, SshConstants.SSH_MSG_CHANNEL_DATA) {
                @SuppressWarnings("synthetic-access")
                @Override
                protected CloseFuture doCloseGracefully() {
                    try {
                        sendEof();
                    } catch (IOException e) {
                        Session session = getSession();
                        session.exceptionCaught(e);
                    }
                    return super.doCloseGracefully();
                }
            };
            asyncOut = new ChannelAsyncInputStream(this);
            asyncErr = new ChannelAsyncInputStream(this);
        } else {
            invertedIn = new ChannelOutputStream(this, getRemoteWindow(), SshConstants.SSH_MSG_CHANNEL_DATA, true);

            Window wLocal = getLocalWindow();
            if (out == null) {
                ChannelPipedInputStream pis = new ChannelPipedInputStream(this, wLocal);
                ChannelPipedOutputStream pos = new ChannelPipedOutputStream(pis);
                out = pos;
                invertedOut = pis;
            }
            if (err == null) {
                ChannelPipedInputStream pis = new ChannelPipedInputStream(this, wLocal);
                ChannelPipedOutputStream pos = new ChannelPipedOutputStream(pis);
                err = pos;
                invertedErr = pis;
            }

            if (in != null) {
                // allocate a temporary executor service if none provided
                ExecutorService service = getExecutorService();
                if (service == null) {
                    pumperService = ThreadUtils.newSingleThreadExecutor("ClientInputStreamPump[" + this.toString() + "]");
                } else {
                    pumperService = service;
                }

                // shutdown the temporary executor service if had to create it
                shutdownPumper = (pumperService != service) || isShutdownOnExit();

                // Interrupt does not really work and the thread will only exit when
                // the call to read() will return.  So ensure this thread is a daemon
                // to avoid blocking the whole app
                pumper = pumperService.submit(this::pumpInputStream);
            }
        }
    }

    @Override
    protected RequestHandler.Result handleInternalRequest(String req, boolean wantReply, Buffer buffer) throws IOException {
        switch (req) {
            case "xon-xoff":
                return handleXonXoff(buffer, wantReply);
            default:
                return super.handleInternalRequest(req, wantReply, buffer);
        }
    }

    // see RFC4254 section 6.8
    protected RequestHandler.Result handleXonXoff(Buffer buffer, boolean wantReply) throws IOException {
        boolean clientCanDo = buffer.getBoolean();

        return RequestHandler.Result.ReplySuccess;
    }

    @Override
    protected void doCloseImmediately() {
        if ((pumper != null) && (pumperService != null) && shutdownPumper && (!pumperService.isShutdown())) {
            try {
                if (!pumper.isDone()) {
                    pumper.cancel(true);
                }

                pumperService.shutdownNow();
            } catch (Exception e) {
            } finally {
                pumper = null;
                pumperService = null;
            }
        }

        super.doCloseImmediately();
    }

    protected void pumpInputStream() {
        try {
            Session session = getSession();
            Window wRemote = getRemoteWindow();
            long packetSize = wRemote.getPacketSize();
            ValidateUtils.checkTrue(packetSize < Integer.MAX_VALUE, "Remote packet size exceeds int boundary: %d", packetSize);
            byte[] buffer = new byte[(int) packetSize];
            while (!closeFuture.isClosed()) {
                int len = securedRead(in, buffer, 0, buffer.length);
                if (len < 0) {
                    sendEof();
                    return;
                }

                session.resetIdleTimeout();
                if (len > 0) {
                    invertedIn.write(buffer, 0, len);
                    invertedIn.flush();
                }
            }

        } catch (Exception e) {
            if (!isClosing()) {
                close(false);
            }
        }
    }

    //
    // On some platforms, a call to System.in.read(new byte[65536], 0,32768) always throws an IOException.
    // So we need to protect against that and chunk the call into smaller calls.
    // This problem was found on Windows, JDK 1.6.0_03-b05.
    //
    protected int securedRead(InputStream in, byte[] buf, int off, int len) throws IOException {
        int n = 0;
        for (; ; ) {
            int nread = in.read(buf, off + n, Math.min(1024, len - n));
            if (nread <= 0) {
                return (n == 0) ? nread : n;
            }
            n += nread;
            if (n >= len) {
                return n;
            }
            // if not closed but no bytes available, return
            if (in.available() <= 0) {
                return n;
            }
        }
    }
}

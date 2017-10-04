package org.xbib.io.sshd.server.channel;

import org.xbib.io.sshd.common.PropertyResolver;
import org.xbib.io.sshd.common.channel.ChannelPipedInputStream;
import org.xbib.io.sshd.common.channel.ChannelPipedOutputStream;
import org.xbib.io.sshd.common.channel.Window;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * {@link ChannelDataReceiver} that buffers the received data into byte buffer
 * and provides an {@link InputStream} to consume them.
 */
public class PipeDataReceiver extends AbstractLoggingBean implements ChannelDataReceiver {
    private InputStream in;
    private OutputStream out;

    public PipeDataReceiver(PropertyResolver resolver, Window localWindow) {
        ChannelPipedInputStream in = new ChannelPipedInputStream(resolver, localWindow);
        this.in = in;
        this.out = new ChannelPipedOutputStream(in);
    }

    public InputStream getIn() {
        return in;
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    @Override
    public int data(ChannelSession channel, byte[] buf, int start, int len) throws IOException {
        out.write(buf, start, len);
        return 0; // ChannelPipedOutputStream calls consume method on its own, so here we return 0 to make the ends meet.
    }
}

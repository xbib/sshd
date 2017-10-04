package org.xbib.io.sshd.server.channel;

import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.channel.ChannelAsyncInputStream;
import org.xbib.io.sshd.common.io.IoInputStream;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;

import java.io.IOException;

/**
 *
 */
public class AsyncDataReceiver implements ChannelDataReceiver {

    private ChannelAsyncInputStream in;

    public AsyncDataReceiver(Channel channel) {
        in = new ChannelAsyncInputStream(channel);
    }

    public IoInputStream getIn() {
        return in;
    }

    @Override
    public int data(ChannelSession channel, byte[] buf, int start, int len) throws IOException {
        in.write(new ByteArrayBuffer(buf, start, len));
        return 0;
    }

    @Override
    public void close() throws IOException {
        in.close(false);
    }
}

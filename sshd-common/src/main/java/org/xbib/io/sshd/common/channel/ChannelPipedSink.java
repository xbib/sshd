package org.xbib.io.sshd.common.channel;

import java.io.IOException;

/**
 *
 */
public interface ChannelPipedSink extends java.nio.channels.Channel {
    /**
     * @param bytes Bytes to be sent to the sink
     * @param off   Offset in buffer
     * @param len   Number of bytes
     * @throws IOException If failed to send the data
     */
    void receive(byte[] bytes, int off, int len) throws IOException;

    /**
     * Signal end of writing to the sink
     */
    void eof();
}

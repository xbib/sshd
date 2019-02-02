package org.xbib.io.sshd.common.io;

import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.nio.channels.Channel;

import java.io.IOException;

public interface PacketWriter extends Channel {
    /**
     * Encode and send the given buffer. <B>Note:</B> for session packets the buffer has to have
     * 5 bytes free at the beginning to allow the encoding to take place. Also, the write position
     * of the buffer has to be set to the position of the last byte to write.
     *
     * @param buffer the buffer to encode and send. <B>NOTE:</B> the buffer must not be touched
     * until the returned write future is completed.
     * @return An {@code IoWriteFuture} that can be used to check when the packet has actually been sent
     * @throws IOException if an error occurred when encoding sending the packet
     */
    IoWriteFuture writePacket(Buffer buffer) throws IOException;
}

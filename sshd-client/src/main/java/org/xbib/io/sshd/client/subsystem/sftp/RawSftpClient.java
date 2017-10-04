package org.xbib.io.sshd.client.subsystem.sftp;

import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.io.IOException;

/**
 *
 */
public interface RawSftpClient {
    /**
     * @param cmd    Command to send - <B>Note:</B> only lower 8-bits are used
     * @param buffer The {@link Buffer} containing the command data
     * @return The assigned request id
     * @throws IOException if failed to send command
     */
    int send(int cmd, Buffer buffer) throws IOException;

    /**
     * @param id The expected request id
     * @return The received response {@link Buffer} containing the request id
     * @throws IOException If connection closed or interrupted
     */
    Buffer receive(int id) throws IOException;
}

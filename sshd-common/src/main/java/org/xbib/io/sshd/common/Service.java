package org.xbib.io.sshd.common;

import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.buffer.Buffer;

/**
 * See RFC 4253 [SSH-TRANS] and the SSH_MSG_SERVICE_REQUEST packet.  Examples include ssh-userauth
 * and ssh-connection but developers are also free to implement their own custom service.
 */
public interface Service extends Closeable {
    Session getSession();

    void start();

    /**
     * Service the request.
     *
     * @param cmd    The incoming command type
     * @param buffer The {@link Buffer} containing optional command parameters
     * @throws Exception If failed to process the command
     */
    void process(int cmd, Buffer buffer) throws Exception;
}

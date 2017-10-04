package org.xbib.io.sshd.common.io;

import org.xbib.io.sshd.common.util.Readable;

/**
 *
 */
public interface IoHandler {

    void sessionCreated(IoSession session) throws Exception;

    void sessionClosed(IoSession session) throws Exception;

    void exceptionCaught(IoSession session, Throwable cause) throws Exception;

    void messageReceived(IoSession session, Readable message) throws Exception;

}

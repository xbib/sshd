package org.xbib.io.sshd.common.io;

import org.xbib.io.sshd.common.Closeable;

/**
 *
 */
public interface IoServiceFactory extends Closeable {

    IoConnector createConnector(IoHandler handler);

    IoAcceptor createAcceptor(IoHandler handler);

}

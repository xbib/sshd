package org.xbib.io.sshd.common.io;

import java.net.SocketAddress;

/**
 *
 */
public interface IoConnector extends IoService {

    IoConnectFuture connect(SocketAddress address);

}

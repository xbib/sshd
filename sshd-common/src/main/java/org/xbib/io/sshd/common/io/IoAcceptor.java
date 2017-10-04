package org.xbib.io.sshd.common.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Set;

/**
 *
 */
public interface IoAcceptor extends IoService {
    int DEFAULT_BACKLOG = 0;

    void bind(Collection<? extends SocketAddress> addresses) throws IOException;

    void bind(SocketAddress address) throws IOException;

    void unbind(Collection<? extends SocketAddress> addresses);

    void unbind(SocketAddress address);

    void unbind();

    Set<SocketAddress> getBoundAddresses();

}

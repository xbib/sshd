package org.xbib.io.sshd.client.session;

import org.xbib.io.sshd.client.config.hosts.HostConfigEntry;
import org.xbib.io.sshd.client.future.ConnectFuture;

import java.io.IOException;
import java.net.SocketAddress;

/**
 */
public interface ClientSessionCreator {
    /**
     * Resolves the <U>effective</U> {@link HostConfigEntry} and connects to it
     *
     * @param username The intended username
     * @param host     The target host name/address - never {@code null}/empty
     * @param port     The target port
     * @return A {@link ConnectFuture}
     * @throws IOException If failed to resolve the effective target or
     *                     connect to it
     * @see #connect(HostConfigEntry)
     */
    ConnectFuture connect(String username, String host, int port) throws IOException;

    /**
     * Resolves the <U>effective</U> {@link HostConfigEntry} and connects to it
     *
     * @param username The intended username
     * @param address  The intended {@link SocketAddress} - never {@code null}. If
     *                 this is an {@link java.net.InetSocketAddress} then the <U>effective</U> {@link HostConfigEntry}
     *                 is resolved and used.
     * @return A {@link ConnectFuture}
     * @throws IOException If failed to resolve the effective target or
     *                     connect to it
     * @see #connect(HostConfigEntry)
     */
    ConnectFuture connect(String username, SocketAddress address) throws IOException;

    /**
     * @param hostConfig The effective {@link HostConfigEntry} to connect to - never {@code null}
     * @return A {@link ConnectFuture}
     * @throws IOException If failed to create the connection future
     */
    ConnectFuture connect(HostConfigEntry hostConfig) throws IOException;
}

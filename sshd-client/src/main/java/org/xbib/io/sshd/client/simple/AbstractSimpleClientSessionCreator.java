package org.xbib.io.sshd.client.simple;

import org.xbib.io.sshd.client.config.hosts.HostConfigEntry;
import org.xbib.io.sshd.client.future.AuthFuture;
import org.xbib.io.sshd.client.future.ConnectFuture;
import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.client.session.ClientSessionCreator;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.Channel;
import java.security.KeyPair;
import java.util.Objects;

/**
 *
 */
public abstract class AbstractSimpleClientSessionCreator extends AbstractSimpleClient implements ClientSessionCreator {
    private long connectTimeout;
    private long authenticateTimeout;

    protected AbstractSimpleClientSessionCreator() {
        this(DEFAULT_CONNECT_TIMEOUT, DEFAULT_AUTHENTICATION_TIMEOUT);
    }

    protected AbstractSimpleClientSessionCreator(long connTimeout, long authTimeout) {
        setConnectTimeout(connTimeout);
        setAuthenticationTimeout(authTimeout);
    }

    /**
     * Wraps an existing {@link ClientSessionCreator} into a {@link org.xbib.io.sshd.client.simple.SimpleClient}
     *
     * @param creator The {@link ClientSessionCreator} - never {@code null}
     * @param channel The {@link Channel} representing the creator for
     *                relaying {@link #isOpen()} and {@link #close()} calls
     * @return The {@link org.xbib.io.sshd.client.simple.SimpleClient} wrapper. <B>Note:</B> closing the wrapper
     * also closes the underlying sessions creator.
     */
    public static SimpleClient wrap(final ClientSessionCreator creator, final Channel channel) {
        Objects.requireNonNull(creator, "No sessions creator");
        Objects.requireNonNull(channel, "No channel");
        return new AbstractSimpleClientSessionCreator() {
            @Override
            public ConnectFuture connect(String username, String host, int port) throws IOException {
                return creator.connect(username, host, port);
            }

            @Override
            public ConnectFuture connect(String username, SocketAddress address) throws IOException {
                return creator.connect(username, address);
            }

            @Override
            public ConnectFuture connect(HostConfigEntry hostConfig) throws IOException {
                return creator.connect(hostConfig);
            }

            @Override
            public boolean isOpen() {
                return channel.isOpen();
            }

            @Override
            public void close() throws IOException {
                channel.close();
            }
        };
    }

    @Override
    public long getConnectTimeout() {
        return connectTimeout;
    }

    @Override
    public void setConnectTimeout(long timeout) {
        ValidateUtils.checkTrue(timeout > 0, "Non-positive connect timeout: %d", timeout);
        connectTimeout = timeout;
    }

    @Override
    public long getAuthenticationTimeout() {
        return authenticateTimeout;
    }

    @Override
    public void setAuthenticationTimeout(long timeout) {
        ValidateUtils.checkTrue(timeout > 0, "Non-positive authentication timeout: %d", timeout);
        authenticateTimeout = timeout;
    }

    @Override
    public ClientSession sessionLogin(SocketAddress target, String username, String password) throws IOException {
        return loginSession(connect(username, target), password);
    }

    @Override
    public ClientSession sessionLogin(SocketAddress target, String username, KeyPair identity) throws IOException {
        return loginSession(connect(username, target), identity);
    }

    protected ClientSession loginSession(ConnectFuture future, String password) throws IOException {
        return authSession(future.verify(getConnectTimeout()), password);
    }

    protected ClientSession loginSession(ConnectFuture future, KeyPair identity) throws IOException {
        return authSession(future.verify(getConnectTimeout()), identity);
    }

    protected ClientSession authSession(ConnectFuture future, String password) throws IOException {
        ClientSession session = future.getSession();
        session.addPasswordIdentity(password.toCharArray());
        return authSession(session);
    }

    protected ClientSession authSession(ConnectFuture future, KeyPair identity) throws IOException {
        ClientSession session = future.getSession();
        session.addPublicKeyIdentity(identity);
        return authSession(session);
    }

    protected ClientSession authSession(ClientSession clientSession) throws IOException {
        ClientSession session = clientSession;
        IOException err = null;
        try {
            AuthFuture auth = session.auth();
            auth.verify(getAuthenticationTimeout());
            session = null; // disable auto-close
        } catch (IOException e) {
            err = GenericUtils.accumulateException(err, e);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (IOException e) {
                    err = GenericUtils.accumulateException(err, e);
                }
            }
        }

        if (err != null) {
            throw err;
        }

        return clientSession;
    }
}

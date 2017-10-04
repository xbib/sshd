package org.xbib.io.sshd.client.simple;

import org.xbib.io.sshd.client.scp.CloseableScpClient;
import org.xbib.io.sshd.client.scp.ScpClient;
import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.util.Objects;

/**
 *
 */
public abstract class AbstractSimpleClient extends AbstractLoggingBean implements SimpleClient {
    protected AbstractSimpleClient() {
        super();
    }

    @Override
    public SftpClient sftpLogin(SocketAddress target, String username, String password) throws IOException {
        return createSftpClient(sessionLogin(target, username, password));
    }

    @Override
    public SftpClient sftpLogin(SocketAddress target, String username, KeyPair identity) throws IOException {
        return createSftpClient(sessionLogin(target, username, identity));
    }

    protected SftpClient createSftpClient(final ClientSession session) throws IOException {
        Exception err = null;
        try {
            SftpClient client = session.createSftpClient();
            try {
                return createSftpClient(session, client);
            } catch (Exception e) {
                err = GenericUtils.accumulateException(err, e);
                try {
                    client.close();
                } catch (Exception t) {
                    err = GenericUtils.accumulateException(err, t);
                }
            }
        } catch (Exception e) {
            err = GenericUtils.accumulateException(err, e);
        }

        try {
            session.close();
        } catch (Exception e) {
            err = GenericUtils.accumulateException(err, e);
        }

        if (err instanceof IOException) {
            throw (IOException) err;
        } else {
            throw new IOException(err);
        }
    }

    protected SftpClient createSftpClient(final ClientSession session, final SftpClient client) throws IOException {
        ClassLoader loader = getClass().getClassLoader();
        Class<?>[] interfaces = {SftpClient.class};
        return (SftpClient) Proxy.newProxyInstance(loader, interfaces, (proxy, method, args) -> {
            Throwable err = null;
            Object result = null;
            String name = method.getName();
            try {
                result = method.invoke(client, args);
            } catch (Throwable t) {
                err = GenericUtils.accumulateException(err, t);
            }

            // propagate the "close" call to the session as well
            if ("close".equals(name) && GenericUtils.isEmpty(args)) {
                try {
                    session.close();
                } catch (Throwable t) {
                    err = GenericUtils.accumulateException(err, t);
                }
            }

            if (err != null) {
                throw err;
            }

            return result;
        });
    }

    @Override
    public CloseableScpClient scpLogin(String host, String username, String password) throws IOException {
        return scpLogin(host, DEFAULT_PORT, username, password);
    }

    @Override
    public CloseableScpClient scpLogin(String host, int port, String username, String password) throws IOException {
        return scpLogin(InetAddress.getByName(ValidateUtils.checkNotNullAndNotEmpty(host, "No host")), port, username, password);
    }

    @Override
    public CloseableScpClient scpLogin(String host, String username, KeyPair identity) throws IOException {
        return scpLogin(host, DEFAULT_PORT, username, identity);
    }

    @Override
    public CloseableScpClient scpLogin(String host, int port, String username, KeyPair identity) throws IOException {
        return scpLogin(InetAddress.getByName(ValidateUtils.checkNotNullAndNotEmpty(host, "No host")), port, username, identity);
    }

    @Override
    public CloseableScpClient scpLogin(InetAddress host, String username, String password) throws IOException {
        return scpLogin(host, DEFAULT_PORT, username, password);
    }

    @Override
    public CloseableScpClient scpLogin(InetAddress host, int port, String username, String password) throws IOException {
        return scpLogin(new InetSocketAddress(Objects.requireNonNull(host, "No host address"), port), username, password);
    }

    @Override
    public CloseableScpClient scpLogin(InetAddress host, String username, KeyPair identity) throws IOException {
        return scpLogin(host, DEFAULT_PORT, username, identity);
    }

    @Override
    public CloseableScpClient scpLogin(InetAddress host, int port, String username, KeyPair identity) throws IOException {
        return scpLogin(new InetSocketAddress(Objects.requireNonNull(host, "No host address"), port), username, identity);
    }

    @Override
    public CloseableScpClient scpLogin(SocketAddress target, String username, String password) throws IOException {
        return createScpClient(sessionLogin(target, username, password));
    }

    @Override
    public CloseableScpClient scpLogin(SocketAddress target, String username, KeyPair identity) throws IOException {
        return createScpClient(sessionLogin(target, username, identity));
    }

    protected CloseableScpClient createScpClient(ClientSession session) throws IOException {
        try {
            ScpClient client = Objects.requireNonNull(session, "No client session").createScpClient();
            ClassLoader loader = getClass().getClassLoader();
            Class<?>[] interfaces = {CloseableScpClient.class};
            return (CloseableScpClient) Proxy.newProxyInstance(loader, interfaces, (proxy, method, args) -> {
                String name = method.getName();
                try {
                    // The Channel implementation is provided by the session
                    if (("close".equals(name) || "isOpen".equals(name)) && GenericUtils.isEmpty(args)) {
                        return method.invoke(session, args);
                    } else {
                        return method.invoke(client, args);
                    }
                } catch (Throwable t) {
                    throw t;
                }
            });
        } catch (Exception e) {
            try {
                session.close();
            } catch (Exception t) {
                e.addSuppressed(t);
            }

            throw GenericUtils.toIOException(e);
        }
    }
}

package org.xbib.io.sshd.client.simple;

import org.xbib.io.sshd.client.scp.CloseableScpClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.channels.Channel;
import java.security.KeyPair;

/**
 * A simplified <U>synchronous</U> API for obtaining SCP sessions.
 */
public interface SimpleScpClient extends SimpleClientConfigurator, Channel {
    /**
     * Creates an SCP session on the default port and logs in using the provided credentials
     *
     * @param host     The target host name or address
     * @param username Username
     * @param password Password
     * @return Created {@link CloseableScpClient} - <B>Note:</B> closing the client also closes its
     * underlying session
     * @throws IOException If failed to login or authenticate
     */
    CloseableScpClient scpLogin(String host, String username, String password) throws IOException;

    /**
     * Creates an SCP session using the provided credentials
     *
     * @param host     The target host name or address
     * @param port     The target port
     * @param username Username
     * @param password Password
     * @return Created {@link CloseableScpClient} - <B>Note:</B> closing the client also closes its
     * underlying session
     * @throws IOException If failed to login or authenticate
     */
    CloseableScpClient scpLogin(String host, int port, String username, String password) throws IOException;

    /**
     * Creates an SCP session on the default port and logs in using the provided credentials
     *
     * @param host     The target host name or address
     * @param username Username
     * @param identity The {@link KeyPair} identity
     * @return Created {@link CloseableScpClient} - <B>Note:</B> closing the client also closes its
     * underlying session
     * @throws IOException If failed to login or authenticate
     */
    CloseableScpClient scpLogin(String host, String username, KeyPair identity) throws IOException;

    /**
     * Creates an SCP session using the provided credentials
     *
     * @param host     The target host name or address
     * @param port     The target port
     * @param username Username
     * @param identity The {@link KeyPair} identity
     * @return Created {@link CloseableScpClient} - <B>Note:</B> closing the client also closes its
     * underlying session
     * @throws IOException If failed to login or authenticate
     */
    CloseableScpClient scpLogin(String host, int port, String username, KeyPair identity) throws IOException;

    /**
     * Creates an SCP session on the default port and logs in using the provided credentials
     *
     * @param host     The target host {@link InetAddress}
     * @param username Username
     * @param password Password
     * @return Created {@link CloseableScpClient} - <B>Note:</B> closing the client also closes its
     * underlying session
     * @throws IOException If failed to login or authenticate
     */
    CloseableScpClient scpLogin(InetAddress host, String username, String password) throws IOException;

    /**
     * Creates an SCP session using the provided credentials
     *
     * @param host     The target host {@link InetAddress}
     * @param port     The target port
     * @param username Username
     * @param password Password
     * @return Created {@link CloseableScpClient} - <B>Note:</B> closing the client also closes its
     * underlying session
     * @throws IOException If failed to login or authenticate
     */
    CloseableScpClient scpLogin(InetAddress host, int port, String username, String password) throws IOException;

    /**
     * Creates an SCP session on the default port and logs in using the provided credentials
     *
     * @param host     The target host {@link InetAddress}
     * @param username Username
     * @param identity The {@link KeyPair} identity
     * @return Created {@link CloseableScpClient} - <B>Note:</B> closing the client also closes its
     * underlying session
     * @throws IOException If failed to login or authenticate
     */
    CloseableScpClient scpLogin(InetAddress host, String username, KeyPair identity) throws IOException;

    /**
     * Creates an SCP session using the provided credentials
     *
     * @param host     The target host {@link InetAddress}
     * @param port     The target port
     * @param username Username
     * @param identity The {@link KeyPair} identity
     * @return Created {@link CloseableScpClient} - <B>Note:</B> closing the client also closes its
     * underlying session
     * @throws IOException If failed to login or authenticate
     */
    CloseableScpClient scpLogin(InetAddress host, int port, String username, KeyPair identity) throws IOException;

    /**
     * Creates an SCP session using the provided credentials
     *
     * @param target   The target {@link SocketAddress}
     * @param username Username
     * @param password Password
     * @return Created {@link CloseableScpClient} - <B>Note:</B> closing the client also closes its
     * underlying session
     * @throws IOException If failed to login or authenticate
     */
    CloseableScpClient scpLogin(SocketAddress target, String username, String password) throws IOException;

    /**
     * Creates an SCP session using the provided credentials
     *
     * @param target   The target {@link SocketAddress}
     * @param username Username
     * @param identity The {@link KeyPair} identity
     * @return Created {@link CloseableScpClient} - <B>Note:</B> closing the client also closes its
     * underlying session
     * @throws IOException If failed to login or authenticate
     */
    CloseableScpClient scpLogin(SocketAddress target, String username, KeyPair identity) throws IOException;
}

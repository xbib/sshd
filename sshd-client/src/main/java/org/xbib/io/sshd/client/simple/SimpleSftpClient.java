package org.xbib.io.sshd.client.simple;

import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.common.util.ValidateUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.Channel;
import java.security.KeyPair;
import java.util.Objects;

/**
 * A simplified <U>synchronous</U> API for obtaining SFTP sessions.
 */
public interface SimpleSftpClient extends SimpleClientConfigurator, Channel {
    /**
     * Creates an SFTP session on the default port and logs in using the provided credentials
     *
     * @param host     The target host name or address
     * @param username Username
     * @param password Password
     * @return Created {@link SftpClient} - <B>Note:</B> closing the client also closes its
     * underlying session
     * @throws IOException If failed to login or authenticate
     */
    default SftpClient sftpLogin(String host, String username, String password) throws IOException {
        return sftpLogin(host, DEFAULT_PORT, username, password);
    }

    /**
     * Creates an SFTP session using the provided credentials
     *
     * @param host     The target host name or address
     * @param port     The target port
     * @param username Username
     * @param password Password
     * @return Created {@link SftpClient} - <B>Note:</B> closing the client also closes its
     * underlying session
     * @throws IOException If failed to login or authenticate
     */
    default SftpClient sftpLogin(String host, int port, String username, String password) throws IOException {
        return sftpLogin(InetAddress.getByName(ValidateUtils.checkNotNullAndNotEmpty(host, "No host")), port, username, password);
    }

    /**
     * Creates an SFTP session on the default port and logs in using the provided credentials
     *
     * @param host     The target host name or address
     * @param username Username
     * @param identity The {@link KeyPair} identity
     * @return Created {@link SftpClient} - <B>Note:</B> closing the client also closes its
     * underlying session
     * @throws IOException If failed to login or authenticate
     */
    default SftpClient sftpLogin(String host, String username, KeyPair identity) throws IOException {
        return sftpLogin(host, DEFAULT_PORT, username, identity);
    }

    /**
     * Creates an SFTP session using the provided credentials
     *
     * @param host     The target host name or address
     * @param port     The target port
     * @param username Username
     * @param identity The {@link KeyPair} identity
     * @return Created {@link SftpClient} - <B>Note:</B> closing the client also closes its
     * underlying session
     * @throws IOException If failed to login or authenticate
     */
    default SftpClient sftpLogin(String host, int port, String username, KeyPair identity) throws IOException {
        return sftpLogin(InetAddress.getByName(ValidateUtils.checkNotNullAndNotEmpty(host, "No host")), port, username, identity);
    }

    /**
     * Creates an SFTP session on the default port and logs in using the provided credentials
     *
     * @param host     The target host {@link InetAddress}
     * @param username Username
     * @param password Password
     * @return Created {@link SftpClient} - <B>Note:</B> closing the client also closes its
     * underlying session
     * @throws IOException If failed to login or authenticate
     */
    default SftpClient sftpLogin(InetAddress host, String username, String password) throws IOException {
        return sftpLogin(host, DEFAULT_PORT, username, password);
    }

    /**
     * Creates an SFTP session using the provided credentials
     *
     * @param host     The target host {@link InetAddress}
     * @param port     The target port
     * @param username Username
     * @param password Password
     * @return Created {@link SftpClient} - <B>Note:</B> closing the client also closes its
     * underlying session
     * @throws IOException If failed to login or authenticate
     */
    default SftpClient sftpLogin(InetAddress host, int port, String username, String password) throws IOException {
        return sftpLogin(new InetSocketAddress(Objects.requireNonNull(host, "No host address"), port), username, password);
    }

    /**
     * Creates an SFTP session on the default port and logs in using the provided credentials
     *
     * @param host     The target host {@link InetAddress}
     * @param username Username
     * @param identity The {@link KeyPair} identity
     * @return Created {@link SftpClient} - <B>Note:</B> closing the client also closes its
     * underlying session
     * @throws IOException If failed to login or authenticate
     */
    default SftpClient sftpLogin(InetAddress host, String username, KeyPair identity) throws IOException {
        return sftpLogin(host, DEFAULT_PORT, username, identity);
    }

    /**
     * Creates an SFTP session using the provided credentials
     *
     * @param host     The target host {@link InetAddress}
     * @param port     The target port
     * @param username Username
     * @param identity The {@link KeyPair} identity
     * @return Created {@link SftpClient} - <B>Note:</B> closing the client also closes its
     * underlying session
     * @throws IOException If failed to login or authenticate
     */
    default SftpClient sftpLogin(InetAddress host, int port, String username, KeyPair identity) throws IOException {
        return sftpLogin(new InetSocketAddress(Objects.requireNonNull(host, "No host address"), port), username, identity);
    }

    /**
     * Creates an SFTP session using the provided credentials
     *
     * @param target   The target {@link SocketAddress}
     * @param username Username
     * @param password Password
     * @return Created {@link SftpClient} - <B>Note:</B> closing the client also closes its
     * underlying session
     * @throws IOException If failed to login or authenticate
     */
    SftpClient sftpLogin(SocketAddress target, String username, String password) throws IOException;

    /**
     * Creates an SFTP session using the provided credentials
     *
     * @param target   The target {@link SocketAddress}
     * @param username Username
     * @param identity The {@link KeyPair} identity
     * @return Created {@link SftpClient} - <B>Note:</B> closing the client also closes its
     * underlying session
     * @throws IOException If failed to login or authenticate
     */
    SftpClient sftpLogin(SocketAddress target, String username, KeyPair identity) throws IOException;

}

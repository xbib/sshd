package org.xbib.io.sshd.client.session;

import org.xbib.io.sshd.client.ClientAuthenticationManager;
import org.xbib.io.sshd.client.ClientFactoryManager;
import org.xbib.io.sshd.client.channel.ChannelExec;
import org.xbib.io.sshd.client.channel.ChannelShell;
import org.xbib.io.sshd.client.channel.ChannelSubsystem;
import org.xbib.io.sshd.client.future.AuthFuture;
import org.xbib.io.sshd.client.scp.ScpClientCreator;
import org.xbib.io.sshd.client.session.forward.DynamicPortForwardingTracker;
import org.xbib.io.sshd.client.session.forward.ExplicitPortForwardingTracker;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClientCreator;
import org.xbib.io.sshd.common.channel.ChannelDirectTcpip;
import org.xbib.io.sshd.common.channel.ClientChannel;
import org.xbib.io.sshd.common.channel.ClientChannelEvent;
import org.xbib.io.sshd.common.forward.PortForwardingManager;
import org.xbib.io.sshd.common.future.KeyExchangeFuture;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.io.NoCloseOutputStream;
import org.xbib.io.sshd.common.util.io.NullOutputStream;
import org.xbib.io.sshd.common.util.net.SshdSocketAddress;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * An authenticated session to a given SSH server.
 * A client session is established using the {@link org.xbib.io.sshd.client.SshClient}.
 * Once the session has been created, the user has to authenticate
 * using either {@link #addPasswordIdentity(char[])} or
 * {@link #addPublicKeyIdentity(java.security.KeyPair)} followed by
 * a call to {@link #auth()}.
 * From this session, channels can be created using the
 * {@link #createChannel(String)} method.  Multiple channels can
 * be created on a given session concurrently.
 * When using the client in an interactive mode, the
 * {@link #waitFor(Collection, long)} method can be used to listen to specific
 * events such as the session being established, authenticated or closed.
 * When a given session is no longer used, it must be closed using the
 * {@link #close(boolean)} method.
 */
public interface ClientSession
        extends Session, ScpClientCreator, SftpClientCreator,
        ClientProxyConnectorHolder, ClientAuthenticationManager,
        PortForwardingManager {
    Set<ClientChannelEvent> REMOTE_COMMAND_WAIT_EVENTS =
            Collections.unmodifiableSet(EnumSet.of(ClientChannelEvent.CLOSED));

    /**
     * Returns the original address (after having been translated through host
     * configuration entries if any) that was request to connect. It contains the
     * original host or address string that was used. <B>Note:</B> this may be
     * different than the result of the {@link #getIoSession()} report of the
     * remote peer
     *
     * @return The original requested address
     */
    SocketAddress getConnectAddress();

    /**
     * Starts the authentication process.
     * User identities will be tried until the server successfully authenticate the user.
     * User identities must be provided before calling this method using
     * {@link #addPasswordIdentity(char[])} or {@link #addPublicKeyIdentity(java.security.KeyPair)}.
     *
     * @return the authentication future
     * @throws IOException if failed to generate the future
     * @see #addPasswordIdentity(char[])
     * @see #addPublicKeyIdentity(java.security.KeyPair)
     */
    AuthFuture auth() throws IOException;

    /**
     * Create a channel of the given type.
     * Same as calling <code>createChannel(type, null)</code>.
     *
     * @param type The channel type
     * @return The created {@link ClientChannel}
     * @throws IOException If failed to create the requested channel
     */
    ClientChannel createChannel(String type) throws IOException;

    /**
     * Create a channel of the given type and sub-type.
     *
     * @param type    The channel type
     * @param subType The channel sub-type
     * @return The created {@link ClientChannel}
     * @throws IOException If failed to create the requested channel
     */
    ClientChannel createChannel(String type, String subType) throws IOException;

    /**
     * Create a channel to start a shell.
     *
     * @return The created {@link ChannelShell}
     * @throws IOException If failed to create the requested channel
     */
    ChannelShell createShellChannel() throws IOException;

    /**
     * Create a channel to execute a command.
     *
     * @param command The command to execute
     * @return The created {@link ChannelExec}
     * @throws IOException If failed to create the requested channel
     */
    ChannelExec createExecChannel(String command) throws IOException;

    /**
     * Execute a command that requires no input and returns its output
     *
     * @param command The command to execute
     * @return The command's standard output result (assumed to be in US-ASCII)
     * @throws IOException If failed to execute the command - including
     *                     if <U>anything</U> was written to the standard error or a non-zero exit
     *                     status was received.
     * @see #executeRemoteCommand(String, OutputStream, Charset)
     */
    default String executeRemoteCommand(String command) throws IOException {
        try (ByteArrayOutputStream stderr = new ByteArrayOutputStream()) {
            String response = executeRemoteCommand(command, stderr, StandardCharsets.US_ASCII);
            if (stderr.size() > 0) {
                byte[] error = stderr.toByteArray();
                String errorMessage = new String(error, StandardCharsets.US_ASCII);
                //throw new RemoteException("Error reported from remote command='" + command, new ServerException(errorMessage));
                throw new RuntimeException(errorMessage);
            }

            return response;
        }
    }

    /**
     * Execute a command that requires no input and returns its output
     *
     * @param command The command to execute - without a terminating LF
     * @param stderr  Standard error output stream - if {@code null} then
     *                error stream data is ignored. <B>Note:</B> if the stream is not {@code null}
     *                then it will be left <U>open</U> when this method returns or exception
     *                is thrown
     * @param charset The command {@link Charset} for input/output/error - if
     *                {@code null} then US_ASCII is assumed
     * @return The command's standard output result
     * @throws IOException If failed to manage the command channel - <B>Note:</B>
     *                     the code does not check if anything was output to the standard error stream,
     *                     but does check the reported exit status (if any) for non-zero value.
     */
    default String executeRemoteCommand(String command, OutputStream stderr, Charset charset) throws IOException {
        if (charset == null) {
            charset = StandardCharsets.US_ASCII;
        }

        try (ByteArrayOutputStream channelOut = new ByteArrayOutputStream(Byte.MAX_VALUE);
             OutputStream channelErr = (stderr == null) ? new NullOutputStream() : new NoCloseOutputStream(stderr);
             ClientChannel channel = createExecChannel(command)) {
            channel.setOut(channelOut);
            channel.setErr(channelErr);
            channel.open().await(); // TODO use verify and a configurable timeout

            // TODO use a configurable timeout
            Collection<ClientChannelEvent> waitMask = channel.waitFor(REMOTE_COMMAND_WAIT_EVENTS, 0L);
            if (waitMask.contains(ClientChannelEvent.TIMEOUT)) {
                throw new SocketTimeoutException("Failed to retrieve command result in time: " + command);
            }

            Integer exitStatus = channel.getExitStatus();
            if ((exitStatus != null) && (exitStatus != 0)) {
                //throw new RemoteException("Remote command failed (" + exitStatus + "): " + command, new ServerException(exitStatus.toString()));
                throw new RuntimeException(exitStatus.toString());
            }

            byte[] response = channelOut.toByteArray();
            return new String(response, charset);
        }
    }

    /**
     * Create a subsystem channel.
     *
     * @param subsystem The subsystem name
     * @return The created {@link ChannelSubsystem}
     * @throws IOException If failed to create the requested channel
     */
    ChannelSubsystem createSubsystemChannel(String subsystem) throws IOException;

    /**
     * Create a direct tcp-ip channel which can be used to stream data to a remote port from the server.
     *
     * @param local  The local address
     * @param remote The remote address
     * @return The created {@link ChannelDirectTcpip}
     * @throws IOException If failed to create the requested channel
     */
    ChannelDirectTcpip createDirectTcpipChannel(SshdSocketAddress local, SshdSocketAddress remote) throws IOException;

    /**
     * Starts a local port forwarding and returns a tracker that stops the
     * forwarding when the {@code close()} method is called. This tracker can
     * be used in a {@code try-with-resource} block to ensure cleanup of the
     * set up forwarding.
     *
     * @param local  The local address
     * @param remote The remote address
     * @return The tracker instance
     * @throws IOException If failed to set up the requested forwarding
     * @see #startLocalPortForwarding(SshdSocketAddress, SshdSocketAddress)
     */
    default ExplicitPortForwardingTracker createLocalPortForwardingTracker(SshdSocketAddress local, SshdSocketAddress remote) throws IOException {
        return new ExplicitPortForwardingTracker(this, true, local, remote, startLocalPortForwarding(local, remote));
    }

    /**
     * Starts a remote port forwarding and returns a tracker that stops the
     * forwarding when the {@code close()} method is called. This tracker can
     * be used in a {@code try-with-resource} block to ensure cleanup of the
     * set up forwarding.
     *
     * @param remote The remote address
     * @param local  The local address
     * @return The tracker instance
     * @throws IOException If failed to set up the requested forwarding
     * @see #startRemotePortForwarding(SshdSocketAddress, SshdSocketAddress)
     */
    default ExplicitPortForwardingTracker createRemotePortForwardingTracker(SshdSocketAddress remote, SshdSocketAddress local) throws IOException {
        return new ExplicitPortForwardingTracker(this, false, local, remote, startRemotePortForwarding(remote, local));
    }

    /**
     * Starts a dynamic port forwarding and returns a tracker that stops the
     * forwarding when the {@code close()} method is called. This tracker can
     * be used in a {@code try-with-resource} block to ensure cleanup of the
     * set up forwarding.
     *
     * @param local The local address
     * @return The tracker instance
     * @throws IOException If failed to set up the requested forwarding
     * @see #startDynamicPortForwarding(SshdSocketAddress)
     */
    default DynamicPortForwardingTracker createDynamicPortForwardingTracker(SshdSocketAddress local) throws IOException {
        return new DynamicPortForwardingTracker(this, local, startDynamicPortForwarding(local));
    }

    /**
     * Wait for any one of a specific state to be signaled.
     *
     * @param mask    The request {@link ClientSessionEvent}s mask
     * @param timeout Wait time in milliseconds - non-positive means forever
     * @return The actual state that was detected either due to the mask
     * yielding one of the states or due to timeout (in which case the {@link ClientSessionEvent#TIMEOUT}
     * value is set)
     */
    Set<ClientSessionEvent> waitFor(Collection<ClientSessionEvent> mask, long timeout);

    /**
     * Access to the metadata.
     *
     * @return The metadata {@link Map}
     */
    Map<Object, Object> getMetadataMap();

    /**
     * @return The ClientFactoryManager for this session.
     */
    @Override
    ClientFactoryManager getFactoryManager();

    /**
     * Switch to a none cipher for performance.
     * This should be done after the authentication phase has been performed.
     * After such a switch, interactive channels are not allowed anymore.
     * Both client and server must have been configured to support the none cipher.
     * If that's not the case, the returned future will be set with an exception.
     *
     * @return an {@link KeyExchangeFuture} that can be used to wait for the exchange
     * to be finished
     * @throws IOException if a key exchange is already running
     */
    KeyExchangeFuture switchToNoneCipher() throws IOException;

    enum ClientSessionEvent {
        TIMEOUT,
        CLOSED,
        WAIT_AUTH,
        AUTHED
    }
}

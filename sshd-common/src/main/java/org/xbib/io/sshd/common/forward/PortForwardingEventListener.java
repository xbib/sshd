package org.xbib.io.sshd.common.forward;

import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.SshdEventListener;
import org.xbib.io.sshd.common.util.net.SshdSocketAddress;

import java.io.IOException;

/**
 *
 */
public interface PortForwardingEventListener extends SshdEventListener {
    PortForwardingEventListener EMPTY = new PortForwardingEventListener() {
        @Override
        public String toString() {
            return "EMPTY";
        }
    };

    static <L extends PortForwardingEventListener> L validateListener(L listener) {
        return SshdEventListener.validateListener(listener, PortForwardingEventListener.class.getSimpleName());
    }

    /**
     * Signals the attempt to establish a local/remote port forwarding
     *
     * @param session         The {@link Session} through which the attempt is made
     * @param local           The local address - may be {@code null} on the receiver side
     * @param remote          The remote address - may be {@code null} on the receiver side
     * @param localForwarding Local/remote port forwarding indicator
     * @throws IOException If failed to handle the event - in which case
     *                     the attempt is aborted and the exception re-thrown to the caller
     */
    default void establishingExplicitTunnel(
            Session session, SshdSocketAddress local, SshdSocketAddress remote, boolean localForwarding)
            throws IOException {
        // ignored
    }

    /**
     * Signals a successful/failed attempt to establish a local/remote port forwarding
     *
     * @param session         The {@link Session} through which the attempt was made
     * @param local           The local address - may be {@code null} on the receiver side
     * @param remote          The remote address - may be {@code null} on the receiver side
     * @param localForwarding Local/remote port forwarding indicator
     * @param boundAddress    The bound address - non-{@code null} if successful
     * @param reason          Reason for failure - {@code null} if successful
     * @throws IOException If failed to handle the event - in which case
     *                     the established tunnel is aborted
     */
    default void establishedExplicitTunnel(
            Session session, SshdSocketAddress local, SshdSocketAddress remote, boolean localForwarding,
            SshdSocketAddress boundAddress, Throwable reason)
            throws IOException {
        // ignored
    }

    /**
     * Signals a request to tear down a local/remote port forwarding
     *
     * @param session         The {@link Session} through which the request is made
     * @param address         The (bound) address - local/remote according to the forwarding type
     * @param localForwarding Local/remote port forwarding indicator
     * @throws IOException If failed to handle the event - in which case
     *                     the request is aborted
     */
    default void tearingDownExplicitTunnel(Session session, SshdSocketAddress address, boolean localForwarding)
            throws IOException {
        // ignored
    }

    /**
     * Signals a successful/failed request to tear down a local/remote port forwarding
     *
     * @param session         The {@link Session} through which the request is made
     * @param address         The (bound) address - local/remote according to the forwarding type
     * @param localForwarding Local/remote port forwarding indicator
     * @param reason          Reason for failure - {@code null} if successful
     * @throws IOException If failed to handle the event - <B>Note:</B>
     *                     the exception is propagated, but the port forwarding may have
     *                     been torn down - no rollback
     */
    default void tornDownExplicitTunnel(Session session, SshdSocketAddress address, boolean localForwarding, Throwable reason)
            throws IOException {
        // ignored
    }

    /**
     * Signals the attempt to establish a dynamic port forwarding
     *
     * @param session The {@link Session} through which the attempt is made
     * @param local   The local address
     * @throws IOException If failed to handle the event - in which case
     *                     the attempt is aborted and the exception re-thrown to the caller
     */
    default void establishingDynamicTunnel(Session session, SshdSocketAddress local) throws IOException {
        // ignored
    }

    /**
     * Signals a successful/failed attempt to establish a dynamic port forwarding
     *
     * @param session      The {@link Session} through which the attempt is made
     * @param local        The local address
     * @param boundAddress The bound address - non-{@code null} if successful
     * @param reason       Reason for failure - {@code null} if successful
     * @throws IOException If failed to handle the event - in which case
     *                     the established tunnel is aborted
     */
    default void establishedDynamicTunnel(
            Session session, SshdSocketAddress local, SshdSocketAddress boundAddress, Throwable reason)
            throws IOException {
        // ignored
    }

    /**
     * Signals a request to tear down a dynamic forwarding
     *
     * @param session The {@link Session} through which the request is made
     * @param address The (bound) address - local/remote according to the forwarding type
     * @throws IOException If failed to handle the event - in which case
     *                     the request is aborted
     */
    default void tearingDownDynamicTunnel(Session session, SshdSocketAddress address) throws IOException {
        // ignored
    }

    /**
     * Signals a successful/failed request to tear down a dynamic port forwarding
     *
     * @param session The {@link Session} through which the request is made
     * @param address The (bound) address - local/remote according to the forwarding type
     * @param reason  Reason for failure - {@code null} if successful
     * @throws IOException If failed to handle the event - <B>Note:</B>
     *                     the exception is propagated, but the port forwarding may have
     *                     been torn down - no rollback
     */
    default void tornDownDynamicTunnel(Session session, SshdSocketAddress address, Throwable reason) throws IOException {
        // ignored
    }
}

package org.xbib.io.sshd.common.forward;

import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.agent.SshAgent;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.net.SshdSocketAddress;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Determines if a forwarding request will be permitted.
 */
public interface ForwardingFilter {
    /**
     * Determine if the session may arrange for agent forwarding.
     * This server process will open a new listen socket locally and export
     * the address in the {@link SshAgent#SSH_AUTHSOCKET_ENV_NAME} environment
     * variable.
     *
     * @param session     The {@link Session} requesting permission to forward the agent.
     * @param requestType The request type string that triggered this call
     * @return true if the agent forwarding is permitted, false if denied.
     */
    boolean canForwardAgent(Session session, String requestType);

    /**
     * Determine if the session may arrange for X11 forwarding.
     * This server process will open a new listen socket locally and export
     * the address in the environment so X11 clients can be tunneled to the
     * user's X11 display server.
     *
     * @param session     The {@link Session} requesting permission to forward X11 connections.
     * @param requestType The request type string that triggered this call
     * @return true if the X11 forwarding is permitted, false if denied.
     */
    boolean canForwardX11(Session session, String requestType);

    /**
     * Determine if the session may listen for inbound connections.
     * This server process will open a new listen socket on the address given
     * by the client (usually 127.0.0.1 but may be any address).  Any inbound
     * connections to this socket will be tunneled over the session to the
     * client, which the client will then forward the connection to another
     * host on the client's side of the network.
     *
     * @param address address the client has requested this server listen
     *                for inbound connections on, and relay them through the client.
     * @param session The {@link Session} requesting permission to listen for connections.
     * @return true if the socket is permitted; false if it must be denied.
     */
    boolean canListen(SshdSocketAddress address, Session session);

    /**
     * Determine if the session may create an outbound connection.
     * This server process will connect to another server listening on the
     * address specified by the client.  Usually this is to another port on
     * the same host (127.0.0.1) but may be to any other system this server
     * can reach on the server's side of the network.
     *
     * @param type    The {@link Type} of requested connection forwarding
     * @param address address the client has requested this server listen
     *                for inbound connections on, and relay them through the client.
     * @param session session requesting permission to listen for connections.
     * @return true if the socket is permitted; false if it must be denied.
     */
    boolean canConnect(Type type, SshdSocketAddress address, Session session);

    /**
     * The type of requested connection forwarding. The type's {@link #getName()}
     * method returns the SSH request type
     */
    enum Type implements NamedResource {
        Direct("direct-tcpip"),
        Forwarded("forwarded-tcpip");

        public static final Set<Type> VALUES =
                Collections.unmodifiableSet(EnumSet.allOf(Type.class));

        private final String name;

        Type(String name) {
            this.name = name;
        }

        /**
         * @param name Either the enum name or the request - ignored if {@code null}/empty
         * @return The matching {@link Type} value - case <U>insensitive</U>,
         * or {@code null} if no match found
         * @see #fromName(String)
         * @see #fromEnumName(String)
         */
        public static Type fromString(String name) {
            if (GenericUtils.isEmpty(name)) {
                return null;
            }

            Type t = fromName(name);
            if (t == null) {
                t = fromEnumName(name);
            }

            return t;
        }

        /**
         * @param name The request name - ignored if {@code null}/empty
         * @return The matching {@link Type} value - case <U>insensitive</U>,
         * or {@code null} if no match found
         */
        public static Type fromName(String name) {
            return NamedResource.findByName(name, String.CASE_INSENSITIVE_ORDER, VALUES);
        }

        /**
         * @param name The enum value name - ignored if {@code null}/empty
         * @return The matching {@link Type} value - case <U>insensitive</U>,
         * or {@code null} if no match found
         */
        public static Type fromEnumName(String name) {
            if (GenericUtils.isEmpty(name)) {
                return null;
            }

            for (Type t : VALUES) {
                if (name.equalsIgnoreCase(t.name())) {
                    return t;
                }
            }

            return null;
        }

        @Override
        public final String getName() {
            return name;
        }
    }
}

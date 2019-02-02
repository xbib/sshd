package org.xbib.io.sshd.client.session;

import org.xbib.io.sshd.client.ClientAuthenticationManager;
import org.xbib.io.sshd.client.auth.UserAuth;
import org.xbib.io.sshd.client.auth.keyboard.UserInteraction;
import org.xbib.io.sshd.client.future.AuthFuture;
import org.xbib.io.sshd.client.future.DefaultAuthFuture;
import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.RuntimeSshException;
import org.xbib.io.sshd.common.Service;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.session.SessionHolder;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.closeable.AbstractCloseable;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Client side <code>ssh-auth</code> service.
 */
public class ClientUserAuthService
        extends AbstractCloseable
        implements Service, SessionHolder<ClientSession>, ClientSessionHolder {

    /**
     * The AuthFuture that is being used by the current auth request.  This encodes the state.
     * isSuccess -> authenticated, else if isDone -> server waiting for user auth, else authenticating.
     */
    private final AtomicReference<AuthFuture> authFutureHolder = new AtomicReference<>();

    private final ClientSessionImpl clientSession;
    private final List<String> clientMethods;
    private final List<NamedFactory<UserAuth>> authFactories;

    private String service;
    private List<String> serverMethods;
    private UserAuth userAuth;
    private int currentMethod;

    public ClientUserAuthService(Session s) {
        clientSession = ValidateUtils.checkInstanceOf(s, ClientSessionImpl.class, "Client side service used on server side: %s", s);
        authFactories = ValidateUtils.checkNotNullAndNotEmpty(
                clientSession.getUserAuthFactories(), "No user auth factories for %s", s);
        clientMethods = new ArrayList<>();

        String prefs = s.getString(ClientAuthenticationManager.PREFERRED_AUTHS);
        if (GenericUtils.isEmpty(prefs)) {
            for (NamedFactory<UserAuth> factory : authFactories) {
                clientMethods.add(factory.getName());
            }
        } else {
            for (String pref : GenericUtils.split(prefs, ',')) {
                NamedFactory<UserAuth> factory = NamedResource.findByName(pref, String.CASE_INSENSITIVE_ORDER, authFactories);
                if (factory != null) {
                    clientMethods.add(pref);
                } else {
                }
            }
        }
    }

    @Override
    public ClientSession getSession() {
        return getClientSession();
    }

    @Override
    public ClientSession getClientSession() {
        return clientSession;
    }

    @Override
    public void start() {
        // ignored
    }

    public AuthFuture auth(String service) throws IOException {
        this.service = ValidateUtils.checkNotNullAndNotEmpty(service, "No service name");

        ClientSession session = getClientSession();
        // check if any previous future in use
        AuthFuture authFuture = new DefaultAuthFuture(service, clientSession.getLock());
        AuthFuture currentFuture = authFutureHolder.getAndSet(authFuture);
        if (currentFuture != null) {
            if (currentFuture.isDone()) {
            } else {
                currentFuture.setException(new InterruptedIOException("New authentication started before previous completed"));
            }
        }

        // start from scratch
        serverMethods = null;
        currentMethod = 0;
        if (userAuth != null) {
            try {
                userAuth.destroy();
            } finally {
                userAuth = null;
            }
        }

        String username = session.getUsername();
        Buffer buffer = session.createBuffer(SshConstants.SSH_MSG_USERAUTH_REQUEST, username.length() + service.length() + Integer.SIZE);
        buffer.putString(username);
        buffer.putString(service);
        buffer.putString("none");
        session.writePacket(buffer);

        return authFuture;
    }

    @Override
    public void process(int cmd, Buffer buffer) throws Exception {
        ClientSession session = getClientSession();
        AuthFuture authFuture = authFutureHolder.get();
        if ((authFuture != null) && authFuture.isSuccess()) {
            throw new IllegalStateException("UserAuth message delivered to authenticated client");
        } else if ((authFuture != null) && authFuture.isDone()) {
        } else if (cmd == SshConstants.SSH_MSG_USERAUTH_BANNER) {
            String welcome = buffer.getString();
            String lang = buffer.getString();
            UserInteraction ui = session.getUserInteraction();
            try {
                if ((ui != null) && ui.isInteractionAllowed(session)) {
                    ui.welcome(session, welcome, lang);
                }
            } catch (Error e) {
                throw new RuntimeSshException(e);
            }
        } else {
            buffer.rpos(buffer.rpos() - 1);
            processUserAuth(buffer);
        }
    }

    /**
     * Execute one step in user authentication.
     *
     * @param buffer The input {@link Buffer}
     * @throws Exception If failed to process
     */
    protected void processUserAuth(Buffer buffer) throws Exception {
        int cmd = buffer.getUByte();
        ClientSession session = getClientSession();
        if (cmd == SshConstants.SSH_MSG_USERAUTH_SUCCESS) {
            if (userAuth != null) {
                try {
                    userAuth.destroy();
                } finally {
                    userAuth = null;
                }
            }
            session.setAuthenticated();
            ((ClientSessionImpl) session).switchToNextService();

            AuthFuture authFuture = Objects.requireNonNull(authFutureHolder.get(), "No current future");
            // Will wake up anyone sitting in waitFor
            authFuture.setAuthed(true);
            return;
        }

        if (cmd == SshConstants.SSH_MSG_USERAUTH_FAILURE) {
            String mths = buffer.getString();
            boolean partial = buffer.getBoolean();
            if (partial || (serverMethods == null)) {
                serverMethods = Arrays.asList(GenericUtils.split(mths, ','));
                currentMethod = 0;
                if (userAuth != null) {
                    try {
                        userAuth.destroy();
                    } finally {
                        userAuth = null;
                    }
                }
            }

            tryNext(cmd);
            return;
        }

        if (userAuth == null) {
            throw new IllegalStateException("Received unknown packet: " + SshConstants.getCommandMessageName(cmd));
        }
        buffer.rpos(buffer.rpos() - 1);
        if (!userAuth.process(buffer)) {
            tryNext(cmd);
        }
    }

    protected void tryNext(int cmd) throws Exception {
        ClientSession session = getClientSession();
        // Loop until we find something to try
        while (true) {
            if (userAuth == null) {
            } else if (!userAuth.process(null)) {
                try {
                    userAuth.destroy();
                } finally {
                    userAuth = null;
                }

                currentMethod++;
            } else {
                return;
            }

            String method = null;
            for (; currentMethod < clientMethods.size(); currentMethod++) {
                method = clientMethods.get(currentMethod);
                if (serverMethods.contains(method)) {
                    break;
                }
            }

            if (currentMethod >= clientMethods.size()) {
                // also wake up anyone sitting in waitFor
                AuthFuture authFuture = Objects.requireNonNull(authFutureHolder.get(), "No current future");
                authFuture.setException(new SshException(SshConstants.SSH2_DISCONNECT_NO_MORE_AUTH_METHODS_AVAILABLE, "No more authentication methods available"));
                return;
            }

            userAuth = NamedFactory.create(authFactories, method);
            if (userAuth == null) {
                throw new UnsupportedOperationException("Failed to find a user-auth factory for method=" + method);
            }
            userAuth.init(session, service);
        }
    }

    @Override
    protected void preClose() {
        AuthFuture authFuture = authFutureHolder.get();
        if ((authFuture != null) && (!authFuture.isDone())) {
            authFuture.setException(new SshException("Session is closed"));
        }

        super.preClose();
    }
}

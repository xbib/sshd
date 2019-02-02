package org.xbib.io.sshd.server.session;

import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.RuntimeSshException;
import org.xbib.io.sshd.common.ServiceFactory;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.auth.AbstractUserAuthServiceFactory;
import org.xbib.io.sshd.common.io.IoService;
import org.xbib.io.sshd.common.io.IoSession;
import org.xbib.io.sshd.common.io.IoWriteFuture;
import org.xbib.io.sshd.common.kex.KexProposalOption;
import org.xbib.io.sshd.common.kex.KexState;
import org.xbib.io.sshd.common.keyprovider.KeyPairProvider;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.session.helpers.AbstractSession;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;
import org.xbib.io.sshd.server.ServerFactoryManager;
import org.xbib.io.sshd.server.auth.UserAuth;
import org.xbib.io.sshd.server.auth.WelcomeBannerPhase;
import org.xbib.io.sshd.server.auth.hostbased.HostBasedAuthenticator;
import org.xbib.io.sshd.server.auth.keyboard.KeyboardInteractiveAuthenticator;
import org.xbib.io.sshd.server.auth.password.PasswordAuthenticator;
import org.xbib.io.sshd.server.auth.pubkey.PublickeyAuthenticator;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provides default implementations for {@link ServerSession} related methods.
 */
public abstract class AbstractServerSession extends AbstractSession implements ServerSession {
    private ServerProxyAcceptor proxyAcceptor;
    private SocketAddress clientAddress;
    private PasswordAuthenticator passwordAuthenticator;
    private PublickeyAuthenticator publickeyAuthenticator;
    private KeyboardInteractiveAuthenticator interactiveAuthenticator;
    private HostBasedAuthenticator hostBasedAuthenticator;
    private List<NamedFactory<UserAuth>> userAuthFactories;

    protected AbstractServerSession(ServerFactoryManager factoryManager, IoSession ioSession) {
        super(true, factoryManager, ioSession);
    }

    @Override
    public ServerFactoryManager getFactoryManager() {
        return (ServerFactoryManager) super.getFactoryManager();
    }

    @Override
    public ServerProxyAcceptor getServerProxyAcceptor() {
        return resolveEffectiveProvider(ServerProxyAcceptor.class, proxyAcceptor, getFactoryManager().getServerProxyAcceptor());
    }

    @Override
    public void setServerProxyAcceptor(ServerProxyAcceptor proxyAcceptor) {
        this.proxyAcceptor = proxyAcceptor;
    }

    @Override
    public SocketAddress getClientAddress() {
        return resolvePeerAddress(clientAddress);
    }

    public void setClientAddress(SocketAddress clientAddress) {
        this.clientAddress = clientAddress;
    }

    @Override
    public PasswordAuthenticator getPasswordAuthenticator() {
        return resolveEffectiveProvider(PasswordAuthenticator.class, passwordAuthenticator, getFactoryManager().getPasswordAuthenticator());
    }

    @Override
    public void setPasswordAuthenticator(PasswordAuthenticator passwordAuthenticator) {
        this.passwordAuthenticator = passwordAuthenticator; // OK if null - inherit from parent
    }

    @Override
    public PublickeyAuthenticator getPublickeyAuthenticator() {
        return resolveEffectiveProvider(PublickeyAuthenticator.class, publickeyAuthenticator, getFactoryManager().getPublickeyAuthenticator());
    }

    @Override
    public void setPublickeyAuthenticator(PublickeyAuthenticator publickeyAuthenticator) {
        this.publickeyAuthenticator = publickeyAuthenticator; // OK if null - inherit from parent
    }

    @Override
    public KeyboardInteractiveAuthenticator getKeyboardInteractiveAuthenticator() {
        return resolveEffectiveProvider(KeyboardInteractiveAuthenticator.class, interactiveAuthenticator, getFactoryManager().getKeyboardInteractiveAuthenticator());
    }

    @Override
    public void setKeyboardInteractiveAuthenticator(KeyboardInteractiveAuthenticator interactiveAuthenticator) {
        this.interactiveAuthenticator = interactiveAuthenticator; // OK if null - inherit from parent
    }

    @Override
    public HostBasedAuthenticator getHostBasedAuthenticator() {
        return resolveEffectiveProvider(HostBasedAuthenticator.class, hostBasedAuthenticator, getFactoryManager().getHostBasedAuthenticator());
    }

    @Override
    public void setHostBasedAuthenticator(HostBasedAuthenticator hostBasedAuthenticator) {
        this.hostBasedAuthenticator = hostBasedAuthenticator;
    }

    @Override
    public List<NamedFactory<UserAuth>> getUserAuthFactories() {
        return resolveEffectiveFactories(UserAuth.class, userAuthFactories, getFactoryManager().getUserAuthFactories());
    }

    @Override
    public void setUserAuthFactories(List<NamedFactory<UserAuth>> userAuthFactories) {
        this.userAuthFactories = userAuthFactories; // OK if null/empty - inherit from parent
    }

    /**
     * Sends the server identification + any extra header lines
     *
     * @param headerLines Extra header lines to be prepended to the actual
     *                    identification string - ignored if {@code null}/empty
     * @return An {@link IoWriteFuture} that can be used to be notified of
     * identification data being written successfully or failing
     * @see <A HREF="https://tools.ietf.org/html/rfc4253#section-4.2">RFC 4253 - section 4.2</A>
     */
    protected IoWriteFuture sendServerIdentification(String... headerLines) throws IOException {
        serverVersion = resolveIdentificationString(ServerFactoryManager.SERVER_IDENTIFICATION);

        String ident = serverVersion;
        if (GenericUtils.length(headerLines) > 0) {
            ident = GenericUtils.join(headerLines, "\r\n") + "\r\n" + serverVersion;
        }
        return sendIdentification(ident);
    }

    @Override
    protected void checkKeys() {
        // nothing
    }

    @Override
    protected boolean handleServiceRequest(String serviceName, Buffer buffer) throws Exception {
        boolean started = super.handleServiceRequest(serviceName, buffer);
        if (!started) {
            return false;
        }

        if (AbstractUserAuthServiceFactory.DEFAULT_NAME.equals(serviceName)
                && (currentService instanceof ServerUserAuthService)) {
            ServerUserAuthService authService = (ServerUserAuthService) currentService;
            if (WelcomeBannerPhase.IMMEDIATE.equals(authService.getWelcomePhase())) {
                authService.sendWelcomeBanner(this);
            }
        }

        return true;
    }

    @Override
    public void startService(String name) throws Exception {
        FactoryManager factoryManager = getFactoryManager();
        currentService = ServiceFactory.create(
                factoryManager.getServiceFactories(),
                ValidateUtils.checkNotNullAndNotEmpty(name, "No service name"),
                this);
        /*
         * According to RFC4253:
         *
         *      If the server rejects the service request, it SHOULD send an
         *      appropriate SSH_MSG_DISCONNECT message and MUST disconnect.
         */
        if (currentService == null) {
            throw new SshException(SshConstants.SSH2_DISCONNECT_SERVICE_NOT_AVAILABLE, "Unknown service: " + name);
        }
    }

    @Override
    protected void handleServiceAccept(String serviceName, Buffer buffer) throws Exception {
        super.handleServiceAccept(serviceName, buffer);
        // TODO: can services be initiated by the server-side ?
        disconnect(SshConstants.SSH2_DISCONNECT_PROTOCOL_ERROR, "Unsupported packet: SSH_MSG_SERVICE_ACCEPT for " + serviceName);
    }

    @Override
    protected byte[] sendKexInit(Map<KexProposalOption, String> proposal) throws IOException {
        mergeProposals(serverProposal, proposal);
        return super.sendKexInit(proposal);
    }

    @Override
    protected void setKexSeed(byte... seed) {
        i_s = ValidateUtils.checkNotNullAndNotEmpty(seed, "No KEX seed");
    }

    @Override
    protected String resolveAvailableSignaturesProposal(FactoryManager proposedManager) {
        /*
         * Make sure we can provide key(s) for the available signatures
         */
        ValidateUtils.checkTrue(proposedManager == getFactoryManager(), "Mismatched signatures proposed factory manager");

        KeyPairProvider kpp = getKeyPairProvider();
        Collection<String> supported = NamedResource.getNameList(getSignatureFactories());
        Iterable<String> provided;
        try {
            provided = (kpp == null) ? null : kpp.getKeyTypes();
        } catch (Error e) {
            throw new RuntimeSshException(e);
        }

        if ((provided == null) || GenericUtils.isEmpty(supported)) {
            return resolveEmptySignaturesProposal(supported, provided);
        }

        StringBuilder resolveKeys = null;
        for (String keyType : provided) {
            if (!supported.contains(keyType)) {
                continue;
            }

            if (resolveKeys == null) {
                resolveKeys = new StringBuilder(supported.size() * 16 /* ecdsa-sha2-xxxx */);
            }

            if (resolveKeys.length() > 0) {
                resolveKeys.append(',');
            }

            resolveKeys.append(keyType);
        }

        if (GenericUtils.isEmpty(resolveKeys)) {
            return resolveEmptySignaturesProposal(supported, provided);
        } else {
            return resolveKeys.toString();
        }
    }

    /**
     * Called by {@link #resolveAvailableSignaturesProposal(FactoryManager)}
     * if none of the provided keys is supported - last chance for the derived
     * implementation to do something
     *
     * @param supported The supported key types - may be {@code null}/empty
     * @param provided  The available signature types - may be {@code null}/empty
     * @return The resolved proposal - {@code null} by default
     */
    protected String resolveEmptySignaturesProposal(Iterable<String> supported, Iterable<String> provided) {
        return null;
    }

    @Override
    protected boolean readIdentification(Buffer buffer) throws IOException {
        ServerProxyAcceptor acceptor = getServerProxyAcceptor();
        int rpos = buffer.rpos();
        if (acceptor != null) {
            try {
                boolean completed = acceptor.acceptServerProxyMetadata(this, buffer);
                if (!completed) {
                    buffer.rpos(rpos);  // restore original buffer position
                    return false;   // more data required
                }
            } catch (Throwable t) {
                if (t instanceof IOException) {
                    throw (IOException) t;
                } else {
                    throw new SshException(t);
                }
            }
        }

        List<String> ident = doReadIdentification(buffer, true);
        int numLines = GenericUtils.size(ident);
        clientVersion = (numLines <= 0) ? null : ident.remove(numLines - 1);
        if (GenericUtils.isEmpty(clientVersion)) {
            buffer.rpos(rpos);  // restore original buffer position
            return false;   // more data required
        }
        String errorMessage = null;
        if (!Session.isValidVersionPrefix(clientVersion)) {
            errorMessage = "Unsupported protocol version: " + clientVersion;
        }

        /*
         * NOTE: because of the way that "doReadIdentification" works we are
         * assured that there are no extra lines beyond the version one, but
         * we check this nevertheless
         */
        if ((errorMessage == null) && (numLines > 1)) {
            errorMessage = "Unexpected extra " + (numLines - 1) + " lines from client=" + clientVersion;
        }

        if (GenericUtils.length(errorMessage) > 0) {
            ioSession.writePacket(new ByteArrayBuffer((errorMessage + "\n").getBytes(StandardCharsets.UTF_8)))
                    .addListener(future -> close(true));
            throw new SshException(errorMessage);
        }

        kexState.set(KexState.INIT);
        sendKexInit();
        return true;
    }

    @Override
    protected void receiveKexInit(Map<KexProposalOption, String> proposal, byte[] seed) throws IOException {
        mergeProposals(clientProposal, proposal);
        i_c = seed;
    }

    @Override
    public KeyPair getHostKey() {
        String keyType = getNegotiatedKexParameter(KexProposalOption.SERVERKEYS);
        KeyPairProvider provider = Objects.requireNonNull(getKeyPairProvider(), "No host keys provider");
        try {
            return provider.loadKey(keyType);
        } catch (Error e) {
            throw new RuntimeSshException(e);
        }
    }

    @Override
    public int getActiveSessionCountForUser(String userName) {
        if (GenericUtils.isEmpty(userName)) {
            return 0;
        }

        IoService service = ioSession.getService();
        Map<?, IoSession> sessionsMap = service.getManagedSessions();
        if (GenericUtils.isEmpty(sessionsMap)) {
            return 0;
        }

        int totalCount = 0;
        for (IoSession is : sessionsMap.values()) {
            ServerSession session = (ServerSession) getSession(is, true);
            if (session == null) {
                continue;
            }

            String sessionUser = session.getUsername();
            if ((!GenericUtils.isEmpty(sessionUser)) && Objects.equals(sessionUser, userName)) {
                totalCount++;
            }
        }

        return totalCount;
    }

    /**
     * Returns the session id.
     *
     * @return The session id.
     */
    public long getId() {
        return ioSession.getId();
    }
}

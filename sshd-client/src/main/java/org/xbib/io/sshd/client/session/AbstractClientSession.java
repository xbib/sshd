package org.xbib.io.sshd.client.session;

import org.xbib.io.sshd.client.ClientFactoryManager;
import org.xbib.io.sshd.client.auth.AuthenticationIdentitiesProvider;
import org.xbib.io.sshd.client.auth.UserAuth;
import org.xbib.io.sshd.client.auth.keyboard.UserInteraction;
import org.xbib.io.sshd.client.auth.password.PasswordIdentityProvider;
import org.xbib.io.sshd.client.channel.ChannelExec;
import org.xbib.io.sshd.client.channel.ChannelShell;
import org.xbib.io.sshd.client.channel.ChannelSubsystem;
import org.xbib.io.sshd.client.keyverifier.ServerKeyVerifier;
import org.xbib.io.sshd.client.scp.DefaultScpClient;
import org.xbib.io.sshd.client.scp.ScpClient;
import org.xbib.io.sshd.client.subsystem.sftp.DefaultSftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.SftpVersionSelector;
import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.RuntimeSshException;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.channel.ChannelDirectTcpip;
import org.xbib.io.sshd.common.channel.ClientChannel;
import org.xbib.io.sshd.common.cipher.BuiltinCiphers;
import org.xbib.io.sshd.common.cipher.CipherNone;
import org.xbib.io.sshd.common.forward.TcpipForwarder;
import org.xbib.io.sshd.common.future.DefaultKeyExchangeFuture;
import org.xbib.io.sshd.common.future.KeyExchangeFuture;
import org.xbib.io.sshd.common.io.IoSession;
import org.xbib.io.sshd.common.io.IoWriteFuture;
import org.xbib.io.sshd.common.kex.KexProposalOption;
import org.xbib.io.sshd.common.kex.KexState;
import org.xbib.io.sshd.common.scp.ScpFileOpener;
import org.xbib.io.sshd.common.scp.ScpTransferEventListener;
import org.xbib.io.sshd.common.session.ConnectionService;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.session.helpers.AbstractConnectionService;
import org.xbib.io.sshd.common.session.helpers.AbstractSession;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.net.SshdSocketAddress;

import java.io.IOException;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Provides default implementations of {@link org.xbib.io.sshd.client.session.ClientSession} related methods.
 */
public abstract class AbstractClientSession extends AbstractSession implements ClientSession {

    private final List<Object> identities = new CopyOnWriteArrayList<>();

    private final AuthenticationIdentitiesProvider identitiesProvider;

    private ServerKeyVerifier serverKeyVerifier;

    private UserInteraction userInteraction;

    private PasswordIdentityProvider passwordIdentityProvider;

    private List<NamedFactory<UserAuth>> userAuthFactories;

    private ScpTransferEventListener scpListener;

    private ScpFileOpener scpOpener;

    private SocketAddress connectAddress;

    private ClientProxyConnector proxyConnector;

    protected AbstractClientSession(ClientFactoryManager factoryManager, IoSession ioSession) {
        super(false, factoryManager, ioSession);
        identitiesProvider = AuthenticationIdentitiesProvider.wrap(identities);
    }

    @Override
    public ClientFactoryManager getFactoryManager() {
        return (ClientFactoryManager) super.getFactoryManager();
    }

    @Override
    public SocketAddress getConnectAddress() {
        return resolvePeerAddress(connectAddress);
    }

    public void setConnectAddress(SocketAddress connectAddress) {
        this.connectAddress = connectAddress;
    }

    @Override
    public ServerKeyVerifier getServerKeyVerifier() {
        return resolveEffectiveProvider(ServerKeyVerifier.class, serverKeyVerifier, getFactoryManager().getServerKeyVerifier());
    }

    @Override
    public void setServerKeyVerifier(ServerKeyVerifier serverKeyVerifier) {
        this.serverKeyVerifier = serverKeyVerifier; // OK if null - inherit from parent
    }

    @Override
    public UserInteraction getUserInteraction() {
        return resolveEffectiveProvider(UserInteraction.class, userInteraction, getFactoryManager().getUserInteraction());
    }

    @Override
    public void setUserInteraction(UserInteraction userInteraction) {
        this.userInteraction = userInteraction; // OK if null - inherit from parent
    }

    @Override
    public List<NamedFactory<UserAuth>> getUserAuthFactories() {
        return resolveEffectiveFactories(UserAuth.class, userAuthFactories, getFactoryManager().getUserAuthFactories());
    }

    @Override
    public void setUserAuthFactories(List<NamedFactory<UserAuth>> userAuthFactories) {
        this.userAuthFactories = userAuthFactories; // OK if null/empty - inherit from parent
    }

    @Override
    public AuthenticationIdentitiesProvider getRegisteredIdentities() {
        return identitiesProvider;
    }

    @Override
    public PasswordIdentityProvider getPasswordIdentityProvider() {
        return resolveEffectiveProvider(PasswordIdentityProvider.class, passwordIdentityProvider, getFactoryManager().getPasswordIdentityProvider());
    }

    @Override
    public void setPasswordIdentityProvider(PasswordIdentityProvider provider) {
        passwordIdentityProvider = provider;
    }

    @Override
    public ClientProxyConnector getClientProxyConnector() {
        return resolveEffectiveProvider(ClientProxyConnector.class, proxyConnector, getFactoryManager().getClientProxyConnector());
    }

    @Override
    public void setClientProxyConnector(ClientProxyConnector proxyConnector) {
        this.proxyConnector = proxyConnector;
    }

    @Override
    public void addPasswordIdentity(char[] password) {
        ValidateUtils.checkTrue((password != null), "No password provided");
        identities.add(new String(password));
    }

    @Override
    public void addPublicKeyIdentity(KeyPair kp) {
        Objects.requireNonNull(kp, "No key-pair to add");
        Objects.requireNonNull(kp.getPublic(), "No public key");
        Objects.requireNonNull(kp.getPrivate(), "No private key");
        identities.add(kp);

    }

    @Override
    public KeyPair removePublicKeyIdentity(KeyPair kp) {
        if (kp == null) {
            return null;
        }
        int index = AuthenticationIdentitiesProvider.findIdentityIndex(identities,
                AuthenticationIdentitiesProvider.KEYPAIR_IDENTITY_COMPARATOR, kp);
        if (index >= 0) {
            return (KeyPair) identities.remove(index);
        } else {
            return null;
        }
    }

    protected IoWriteFuture sendClientIdentification() throws Exception {
        clientVersion = resolveIdentificationString(ClientFactoryManager.CLIENT_IDENTIFICATION);
        ClientProxyConnector proxyConnector = getClientProxyConnector();
        if (proxyConnector != null) {
            try {
                proxyConnector.sendClientProxyMetadata(this);
            } catch (Throwable t) {
                if (t instanceof Exception) {
                    throw (Exception) t;
                } else {
                    throw new RuntimeSshException(t);
                }
            }
        }
        return sendIdentification(clientVersion);
    }

    @Override
    public ClientChannel createChannel(String type) throws IOException {
        return createChannel(type, null);
    }

    @Override
    public ClientChannel createChannel(String type, String subType) throws IOException {
        if (Channel.CHANNEL_SHELL.equals(type)) {
            return createShellChannel();
        } else if (Channel.CHANNEL_EXEC.equals(type)) {
            return createExecChannel(subType);
        } else if (Channel.CHANNEL_SUBSYSTEM.equals(type)) {
            return createSubsystemChannel(subType);
        } else {
            throw new IllegalArgumentException("Unsupported channel type " + type);
        }
    }

    @Override
    public ChannelExec createExecChannel(String command) throws IOException {
        ChannelExec channel = new ChannelExec(command);
        ConnectionService service = getConnectionService();
        int id = service.registerChannel(channel);
        return channel;
    }

    @Override
    public ChannelSubsystem createSubsystemChannel(String subsystem) throws IOException {
        ChannelSubsystem channel = new ChannelSubsystem(subsystem);
        ConnectionService service = getConnectionService();
        int id = service.registerChannel(channel);
        return channel;
    }

    @Override
    public ChannelDirectTcpip createDirectTcpipChannel(SshdSocketAddress local, SshdSocketAddress remote) throws IOException {
        ChannelDirectTcpip channel = new ChannelDirectTcpip(local, remote);
        ConnectionService service = getConnectionService();
        int id = service.registerChannel(channel);
        return channel;
    }

    protected ClientUserAuthService getUserAuthService() {
        return getService(ClientUserAuthService.class);
    }

    protected ConnectionService getConnectionService() {
        return getService(ConnectionService.class);
    }

    @Override
    public ScpFileOpener getScpFileOpener() {
        return resolveEffectiveProvider(ScpFileOpener.class, scpOpener, getFactoryManager().getScpFileOpener());
    }

    @Override
    public void setScpFileOpener(ScpFileOpener opener) {
        scpOpener = opener;
    }

    @Override
    public ScpTransferEventListener getScpTransferEventListener() {
        return scpListener;
    }

    @Override
    public void setScpTransferEventListener(ScpTransferEventListener listener) {
        scpListener = listener;
    }

    @Override
    public ScpClient createScpClient(ScpFileOpener opener, ScpTransferEventListener listener) {
        return new DefaultScpClient(this, opener, listener);
    }

    @Override
    public SftpClient createSftpClient(SftpVersionSelector selector) throws IOException {
        DefaultSftpClient client = new DefaultSftpClient(this);
        try {
            client.negotiateVersion(selector);
        } catch (IOException | RuntimeException e) {
            client.close();
            throw e;
        }
        return client;
    }

    @Override
    public SshdSocketAddress startLocalPortForwarding(SshdSocketAddress local, SshdSocketAddress remote) throws IOException {
        return getTcpipForwarder().startLocalPortForwarding(local, remote);
    }

    @Override
    public void stopLocalPortForwarding(SshdSocketAddress local) throws IOException {
        getTcpipForwarder().stopLocalPortForwarding(local);
    }

    @Override
    public SshdSocketAddress startRemotePortForwarding(SshdSocketAddress remote, SshdSocketAddress local) throws IOException {
        return getTcpipForwarder().startRemotePortForwarding(remote, local);
    }

    @Override
    public void stopRemotePortForwarding(SshdSocketAddress remote) throws IOException {
        getTcpipForwarder().stopRemotePortForwarding(remote);
    }

    @Override
    public SshdSocketAddress startDynamicPortForwarding(SshdSocketAddress local) throws IOException {
        return getTcpipForwarder().startDynamicPortForwarding(local);
    }

    @Override
    public void stopDynamicPortForwarding(SshdSocketAddress local) throws IOException {
        getTcpipForwarder().stopDynamicPortForwarding(local);
    }

    protected TcpipForwarder getTcpipForwarder() {
        ConnectionService service = Objects.requireNonNull(getConnectionService(), "No connection service");
        return Objects.requireNonNull(service.getTcpipForwarder(), "No forwarder");
    }

    @Override
    protected String resolveAvailableSignaturesProposal(FactoryManager manager) {
        // the client does not have to provide keys for the available signatures
        ValidateUtils.checkTrue(manager == getFactoryManager(), "Mismatched factory manager instances");
        return NamedResource.getNames(getSignatureFactories());
    }

    @Override
    public void startService(String name) throws Exception {
        throw new IllegalStateException("Starting services is not supported on the client side: " + name);
    }

    @Override
    public ChannelShell createShellChannel() throws IOException {
        if ((inCipher instanceof CipherNone) || (outCipher instanceof CipherNone)) {
            throw new IllegalStateException("Interactive channels are not supported with none cipher");
        }

        ChannelShell channel = new ChannelShell();
        ConnectionService service = getConnectionService();
        int id = service.registerChannel(channel);
        return channel;
    }

    @Override
    protected boolean readIdentification(Buffer buffer) throws IOException {
        List<String> ident = doReadIdentification(buffer, false);
        int numLines = GenericUtils.size(ident);
        serverVersion = (numLines <= 0) ? null : ident.remove(numLines - 1);
        if (serverVersion == null) {
            return false;
        }
        if (!Session.isValidVersionPrefix(serverVersion)) {
            throw new SshException(SshConstants.SSH2_DISCONNECT_PROTOCOL_VERSION_NOT_SUPPORTED,
                    "Unsupported protocol version: " + serverVersion);
        }

        signalExtraServerVersionInfo(ident);
        return true;
    }

    protected void signalExtraServerVersionInfo(List<String> lines) throws IOException {
        if (GenericUtils.isEmpty(lines)) {
            return;
        }

        UserInteraction ui = getUserInteraction();
        try {
            if ((ui != null) && ui.isInteractionAllowed(this)) {
                ui.serverVersionInfo(this, lines);
            }
        } catch (Error e) {
            throw new RuntimeSshException(e);
        }
    }

    @Override
    protected byte[] sendKexInit(Map<KexProposalOption, String> proposal) throws IOException {
        mergeProposals(clientProposal, proposal);
        return super.sendKexInit(proposal);
    }

    @Override
    protected void setKexSeed(byte... seed) {
        i_c = ValidateUtils.checkNotNullAndNotEmpty(seed, "No KEX seed");
    }

    @Override
    protected void receiveKexInit(Map<KexProposalOption, String> proposal, byte[] seed) throws IOException {
        mergeProposals(serverProposal, proposal);
        i_s = seed;
    }

    @Override
    protected void checkKeys() throws SshException {
        ServerKeyVerifier serverKeyVerifier = Objects.requireNonNull(getServerKeyVerifier(), "No server key verifier");
        SocketAddress remoteAddress = ioSession.getRemoteAddress();
        PublicKey serverKey = kex.getServerKey();
        boolean verified = serverKeyVerifier.verifyServerKey(this, remoteAddress, serverKey);
        if (!verified) {
            throw new SshException(SshConstants.SSH2_DISCONNECT_HOST_KEY_NOT_VERIFIABLE, "Server key did not validate");
        }
    }

    @Override
    public KeyExchangeFuture switchToNoneCipher() throws IOException {
        if (!(currentService instanceof AbstractConnectionService<?>)
                || !GenericUtils.isEmpty(((AbstractConnectionService<?>) currentService).getChannels())) {
            throw new IllegalStateException("The switch to the none cipher must be done immediately after authentication");
        }

        if (kexState.compareAndSet(KexState.DONE, KexState.INIT)) {
            DefaultKeyExchangeFuture kexFuture = new DefaultKeyExchangeFuture(null);
            DefaultKeyExchangeFuture prev = kexFutureHolder.getAndSet(kexFuture);
            if (prev != null) {
                synchronized (prev) {
                    Object value = prev.getValue();
                    if (value == null) {
                        prev.setValue(new SshException("Switch to none cipher while previous KEX is ongoing"));
                    }
                }
            }

            String c2sEncServer;
            String s2cEncServer;
            synchronized (serverProposal) {
                c2sEncServer = serverProposal.get(KexProposalOption.C2SENC);
                s2cEncServer = serverProposal.get(KexProposalOption.S2CENC);
            }
            boolean c2sEncServerNone = BuiltinCiphers.Constants.isNoneCipherIncluded(c2sEncServer);
            boolean s2cEncServerNone = BuiltinCiphers.Constants.isNoneCipherIncluded(s2cEncServer);

            String c2sEncClient;
            String s2cEncClient;
            synchronized (clientProposal) {
                c2sEncClient = clientProposal.get(KexProposalOption.C2SENC);
                s2cEncClient = clientProposal.get(KexProposalOption.S2CENC);
            }

            boolean c2sEncClientNone = BuiltinCiphers.Constants.isNoneCipherIncluded(c2sEncClient);
            boolean s2cEncClientNone = BuiltinCiphers.Constants.isNoneCipherIncluded(s2cEncClient);

            if ((!c2sEncServerNone) || (!s2cEncServerNone)) {
                kexFuture.setValue(new SshException("Server does not support none cipher"));
            } else if ((!c2sEncClientNone) || (!s2cEncClientNone)) {
                kexFuture.setValue(new SshException("Client does not support none cipher"));
            } else {

                Map<KexProposalOption, String> proposal = new EnumMap<>(KexProposalOption.class);
                synchronized (clientProposal) {
                    proposal.putAll(clientProposal);
                }

                proposal.put(KexProposalOption.C2SENC, BuiltinCiphers.Constants.NONE);
                proposal.put(KexProposalOption.S2CENC, BuiltinCiphers.Constants.NONE);

                byte[] seed = sendKexInit(proposal);
                setKexSeed(seed);
            }

            return Objects.requireNonNull(kexFutureHolder.get(), "No current KEX future");
        } else {
            throw new SshException("In flight key exchange");
        }
    }
}

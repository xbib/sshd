package org.xbib.io.sshd.client;

import org.xbib.io.sshd.client.config.hosts.DefaultConfigFileHostEntryResolver;
import org.xbib.io.sshd.client.config.hosts.HostConfigEntryResolver;
import org.xbib.io.sshd.client.config.keys.ClientIdentityLoader;
import org.xbib.io.sshd.client.global.OpenSshHostKeysHandler;
import org.xbib.io.sshd.client.kex.DHGClient;
import org.xbib.io.sshd.client.kex.DHGEXClient;
import org.xbib.io.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.xbib.io.sshd.client.keyverifier.ServerKeyVerifier;
import org.xbib.io.sshd.common.BaseBuilder;
import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.channel.RequestHandler;
import org.xbib.io.sshd.common.compression.BuiltinCompressions;
import org.xbib.io.sshd.common.compression.CompressionFactory;
import org.xbib.io.sshd.common.config.keys.FilePasswordProvider;
import org.xbib.io.sshd.common.forward.ForwardedTcpipFactory;
import org.xbib.io.sshd.common.kex.DHFactory;
import org.xbib.io.sshd.common.kex.KeyExchange;
import org.xbib.io.sshd.common.session.ConnectionService;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * SSH Client builder.
 */
public class ClientBuilder extends BaseBuilder<SshClient, ClientBuilder> {

    public static final Function<DHFactory, NamedFactory<KeyExchange>> DH2KEX = factory ->
            factory == null
                    ? null
                    : factory.isGroupExchange()
                    ? DHGEXClient.newFactory(factory)
                    : DHGClient.newFactory(factory);

    // Compression is not enabled by default for the client

    public static final List<CompressionFactory> DEFAULT_COMPRESSION_FACTORIES =
            Collections.unmodifiableList(Collections.singletonList(BuiltinCompressions.none));

    public static final List<NamedFactory<Channel>> DEFAULT_CHANNEL_FACTORIES =
            Collections.unmodifiableList(Collections.singletonList(ForwardedTcpipFactory.INSTANCE));

    public static final List<RequestHandler<ConnectionService>> DEFAULT_GLOBAL_REQUEST_HANDLERS =
            Collections.unmodifiableList(Collections.singletonList(OpenSshHostKeysHandler.INSTANCE));

    public static final ServerKeyVerifier DEFAULT_SERVER_KEY_VERIFIER = AcceptAllServerKeyVerifier.INSTANCE;
    public static final HostConfigEntryResolver DEFAULT_HOST_CONFIG_ENTRY_RESOLVER = DefaultConfigFileHostEntryResolver.INSTANCE;
    public static final ClientIdentityLoader DEFAULT_CLIENT_IDENTITY_LOADER = ClientIdentityLoader.DEFAULT;
    public static final FilePasswordProvider DEFAULT_FILE_PASSWORD_PROVIDER = FilePasswordProvider.EMPTY;

    protected ServerKeyVerifier serverKeyVerifier;
    protected HostConfigEntryResolver hostConfigEntryResolver;
    protected ClientIdentityLoader clientIdentityLoader;
    protected FilePasswordProvider filePasswordProvider;

    public ClientBuilder() {
        super();
    }

    /**
     * @param ignoreUnsupported If {@code true} then all the default
     *                          key exchanges are included, regardless of whether they are currently
     *                          supported by the JCE. Otherwise, only the supported ones out of the
     *                          list are included
     * @return A {@link List} of the default {@link NamedFactory}
     * instances of the {@link KeyExchange}s according to the preference
     * order defined by {@link #DEFAULT_KEX_PREFERENCE}.
     * <B>Note:</B> the list may be filtered to exclude unsupported JCE
     * key exchanges according to the {@code ignoreUnsupported} parameter
     * @see org.xbib.io.sshd.common.kex.BuiltinDHFactories#isSupported()
     */
    public static List<NamedFactory<KeyExchange>> setUpDefaultKeyExchanges(boolean ignoreUnsupported) {
        return NamedFactory.setUpTransformedFactories(ignoreUnsupported, DEFAULT_KEX_PREFERENCE, DH2KEX);
    }

    public static ClientBuilder builder() {
        return new ClientBuilder();
    }

    public ClientBuilder serverKeyVerifier(ServerKeyVerifier serverKeyVerifier) {
        this.serverKeyVerifier = serverKeyVerifier;
        return me();
    }

    public ClientBuilder hostConfigEntryResolver(HostConfigEntryResolver resolver) {
        this.hostConfigEntryResolver = resolver;
        return me();
    }

    public ClientBuilder clientIdentityLoader(ClientIdentityLoader loader) {
        this.clientIdentityLoader = loader;
        return me();
    }

    public ClientBuilder filePasswordProvider(FilePasswordProvider provider) {
        this.filePasswordProvider = provider;
        return me();
    }

    @Override
    protected ClientBuilder fillWithDefaultValues() {
        super.fillWithDefaultValues();

        if (compressionFactories == null) {
            compressionFactories = NamedFactory.setUpBuiltinFactories(false, DEFAULT_COMPRESSION_FACTORIES);
        }

        if (keyExchangeFactories == null) {
            keyExchangeFactories = setUpDefaultKeyExchanges(false);
        }

        if (channelFactories == null) {
            channelFactories = DEFAULT_CHANNEL_FACTORIES;
        }

        if (globalRequestHandlers == null) {
            globalRequestHandlers = DEFAULT_GLOBAL_REQUEST_HANDLERS;
        }

        if (serverKeyVerifier == null) {
            serverKeyVerifier = DEFAULT_SERVER_KEY_VERIFIER;
        }

        if (hostConfigEntryResolver == null) {
            hostConfigEntryResolver = DEFAULT_HOST_CONFIG_ENTRY_RESOLVER;
        }

        if (clientIdentityLoader == null) {
            clientIdentityLoader = DEFAULT_CLIENT_IDENTITY_LOADER;
        }

        if (filePasswordProvider == null) {
            filePasswordProvider = DEFAULT_FILE_PASSWORD_PROVIDER;
        }

        if (factory == null) {
            factory = SshClient.DEFAULT_SSH_CLIENT_FACTORY;
        }

        return me();
    }

    @Override
    public SshClient build(boolean isFillWithDefaultValues) {
        SshClient client = super.build(isFillWithDefaultValues);
        client.setServerKeyVerifier(serverKeyVerifier);
        client.setHostConfigEntryResolver(hostConfigEntryResolver);
        client.setClientIdentityLoader(clientIdentityLoader);
        client.setFilePasswordProvider(filePasswordProvider);
        return client;
    }
}
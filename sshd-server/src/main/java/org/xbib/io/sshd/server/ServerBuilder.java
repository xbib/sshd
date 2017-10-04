package org.xbib.io.sshd.server;

import org.xbib.io.sshd.common.BaseBuilder;
import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.channel.RequestHandler;
import org.xbib.io.sshd.common.compression.BuiltinCompressions;
import org.xbib.io.sshd.common.compression.CompressionFactory;
import org.xbib.io.sshd.common.kex.DHFactory;
import org.xbib.io.sshd.common.kex.KeyExchange;
import org.xbib.io.sshd.common.session.ConnectionService;
import org.xbib.io.sshd.server.auth.keyboard.DefaultKeyboardInteractiveAuthenticator;
import org.xbib.io.sshd.server.auth.keyboard.KeyboardInteractiveAuthenticator;
import org.xbib.io.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.xbib.io.sshd.server.channel.ChannelSessionFactory;
import org.xbib.io.sshd.server.config.keys.DefaultAuthorizedKeysAuthenticator;
import org.xbib.io.sshd.server.forward.DirectTcpipFactory;
import org.xbib.io.sshd.server.global.CancelTcpipForwardHandler;
import org.xbib.io.sshd.server.global.KeepAliveHandler;
import org.xbib.io.sshd.server.global.NoMoreSessionsHandler;
import org.xbib.io.sshd.server.global.OpenSshHostKeysHandler;
import org.xbib.io.sshd.server.global.TcpipForwardHandler;
import org.xbib.io.sshd.server.kex.DHGEXServer;
import org.xbib.io.sshd.server.kex.DHGServer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * SshServer builder.
 */
public class ServerBuilder extends BaseBuilder<SshServer, ServerBuilder> {

    public static final Function<DHFactory, NamedFactory<KeyExchange>> DH2KEX = factory ->
            factory == null
                    ? null
                    : factory.isGroupExchange()
                    ? DHGEXServer.newFactory(factory)
                    : DHGServer.newFactory(factory);

    public static final List<NamedFactory<Channel>> DEFAULT_CHANNEL_FACTORIES =
            Collections.unmodifiableList(Arrays.<NamedFactory<Channel>>asList(
                    ChannelSessionFactory.INSTANCE,
                    DirectTcpipFactory.INSTANCE
            ));

    public static final List<RequestHandler<ConnectionService>> DEFAULT_GLOBAL_REQUEST_HANDLERS =
            Collections.unmodifiableList(Arrays.<RequestHandler<ConnectionService>>asList(
                    KeepAliveHandler.INSTANCE,
                    NoMoreSessionsHandler.INSTANCE,
                    TcpipForwardHandler.INSTANCE,
                    CancelTcpipForwardHandler.INSTANCE,
                    OpenSshHostKeysHandler.INSTANCE
            ));

    public static final PublickeyAuthenticator DEFAULT_PUBLIC_KEY_AUTHENTICATOR = DefaultAuthorizedKeysAuthenticator.INSTANCE;
    public static final KeyboardInteractiveAuthenticator DEFAULT_INTERACTIVE_AUTHENTICATOR = DefaultKeyboardInteractiveAuthenticator.INSTANCE;
    public static final List<CompressionFactory> DEFAULT_COMPRESSION_FACTORIES =
            Collections.unmodifiableList(Arrays.<CompressionFactory>asList(
                    BuiltinCompressions.none, BuiltinCompressions.zlib, BuiltinCompressions.delayedZlib));

    protected PublickeyAuthenticator pubkeyAuthenticator;
    protected KeyboardInteractiveAuthenticator interactiveAuthenticator;

    public ServerBuilder() {
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
     * key exchanges according to the <tt>ignoreUnsupported</tt> parameter
     * @see org.xbib.io.sshd.common.kex.BuiltinDHFactories#isSupported()
     */
    public static List<NamedFactory<KeyExchange>> setUpDefaultKeyExchanges(boolean ignoreUnsupported) {
        return NamedFactory.setUpTransformedFactories(ignoreUnsupported, DEFAULT_KEX_PREFERENCE, DH2KEX);
    }

    public static ServerBuilder builder() {
        return new ServerBuilder();
    }

    public ServerBuilder interactiveAuthenticator(KeyboardInteractiveAuthenticator auth) {
        interactiveAuthenticator = auth;
        return this;
    }

    public ServerBuilder publickeyAuthenticator(PublickeyAuthenticator auth) {
        pubkeyAuthenticator = auth;
        return this;
    }

    @Override
    protected ServerBuilder fillWithDefaultValues() {
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

        if (pubkeyAuthenticator == null) {
            pubkeyAuthenticator = DEFAULT_PUBLIC_KEY_AUTHENTICATOR;
        }

        if (interactiveAuthenticator == null) {
            interactiveAuthenticator = DEFAULT_INTERACTIVE_AUTHENTICATOR;
        }

        if (factory == null) {
            factory = SshServer.DEFAULT_SSH_SERVER_FACTORY;
        }

        return me();
    }

    @Override
    public SshServer build(boolean isFillWithDefaultValues) {
        SshServer server = super.build(isFillWithDefaultValues);
        server.setPublickeyAuthenticator(pubkeyAuthenticator);
        server.setKeyboardInteractiveAuthenticator(interactiveAuthenticator);
        return server;
    }
}
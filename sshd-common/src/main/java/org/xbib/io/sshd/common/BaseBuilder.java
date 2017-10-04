package org.xbib.io.sshd.common;

import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.channel.RequestHandler;
import org.xbib.io.sshd.common.cipher.BuiltinCiphers;
import org.xbib.io.sshd.common.cipher.Cipher;
import org.xbib.io.sshd.common.compression.Compression;
import org.xbib.io.sshd.common.file.FileSystemFactory;
import org.xbib.io.sshd.common.file.nativefs.NativeFileSystemFactory;
import org.xbib.io.sshd.common.forward.DefaultTcpipForwarderFactory;
import org.xbib.io.sshd.common.forward.ForwardingFilter;
import org.xbib.io.sshd.common.forward.RejectAllForwardingFilter;
import org.xbib.io.sshd.common.forward.TcpipForwarderFactory;
import org.xbib.io.sshd.common.helpers.AbstractFactoryManager;
import org.xbib.io.sshd.common.kex.BuiltinDHFactories;
import org.xbib.io.sshd.common.kex.KeyExchange;
import org.xbib.io.sshd.common.mac.BuiltinMacs;
import org.xbib.io.sshd.common.mac.Mac;
import org.xbib.io.sshd.common.random.Random;
import org.xbib.io.sshd.common.random.SingletonRandomFactory;
import org.xbib.io.sshd.common.session.ConnectionService;
import org.xbib.io.sshd.common.signature.BuiltinSignatures;
import org.xbib.io.sshd.common.signature.Signature;
import org.xbib.io.sshd.common.util.ObjectBuilder;
import org.xbib.io.sshd.common.util.security.SecurityUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Base class for dedicated client/server instance builders.
 *
 * @param <T> Type of {@link AbstractFactoryManager} being built
 * @param <S> Type of builder
 */
public class BaseBuilder<T extends AbstractFactoryManager, S extends BaseBuilder<T, S>> implements ObjectBuilder<T> {
    public static final FileSystemFactory DEFAULT_FILE_SYSTEM_FACTORY = NativeFileSystemFactory.INSTANCE;

    public static final ForwardingFilter DEFAULT_FORWARDING_FILTER = RejectAllForwardingFilter.INSTANCE;

    public static final TcpipForwarderFactory DEFAULT_FORWARDER_FACTORY = DefaultTcpipForwarderFactory.INSTANCE;

    /**
     * The default {@link BuiltinCiphers} setup in order of preference
     * as specified by <A HREF="https://www.freebsd.org/cgi/man.cgi?query=ssh_config&sektion=5">ssh_config(5)</A>
     */
    public static final List<BuiltinCiphers> DEFAULT_CIPHERS_PREFERENCE =
            Collections.unmodifiableList(Arrays.asList(
                    BuiltinCiphers.aes128ctr,
                    BuiltinCiphers.aes192ctr,
                    BuiltinCiphers.aes256ctr,
                    BuiltinCiphers.arcfour256,
                    BuiltinCiphers.arcfour128,
                    BuiltinCiphers.aes128cbc,
                    BuiltinCiphers.tripledescbc,
                    BuiltinCiphers.blowfishcbc,
                    // TODO add support for cast128-cbc cipher
                    BuiltinCiphers.aes192cbc,
                    BuiltinCiphers.aes256cbc
                    // TODO add support for arcfour cipher
            ));

    /**
     * The default {@link BuiltinDHFactories} setup in order of preference
     * as specified by <A HREF="https://www.freebsd.org/cgi/man.cgi?query=ssh_config&sektion=5">
     * ssh_config(5)</A>
     */
    public static final List<BuiltinDHFactories> DEFAULT_KEX_PREFERENCE =
            Collections.unmodifiableList(Arrays.asList(
                    BuiltinDHFactories.ecdhp521,
                    BuiltinDHFactories.ecdhp384,
                    BuiltinDHFactories.ecdhp256,

                    BuiltinDHFactories.dhgex256,
                    BuiltinDHFactories.dhgex,

                    BuiltinDHFactories.dhg14,
                    BuiltinDHFactories.dhg1
            ));

    /**
     * The default {@link BuiltinMacs} setup in order of preference
     * as specified by <A HREF="https://www.freebsd.org/cgi/man.cgi?query=ssh_config&sektion=5">
     * ssh_config(5)</A>
     */
    public static final List<BuiltinMacs> DEFAULT_MAC_PREFERENCE =
            Collections.unmodifiableList(Arrays.asList(
                    BuiltinMacs.hmacmd5,
                    BuiltinMacs.hmacsha1,
                    BuiltinMacs.hmacsha256,
                    BuiltinMacs.hmacsha512,
                    BuiltinMacs.hmacsha196,
                    BuiltinMacs.hmacmd596
            ));

    /**
     * Preferred {@link BuiltinSignatures} according to
     * <A HREF="https://www.freebsd.org/cgi/man.cgi?query=ssh_config&sektion=5">sshd_config(5)</A>
     * {@code HostKeyAlgorithms} recommendation
     */
    public static final List<BuiltinSignatures> DEFAULT_SIGNATURE_PREFERENCE =
            Collections.unmodifiableList(Arrays.asList(
                    BuiltinSignatures.nistp256,
                    BuiltinSignatures.nistp384,
                    BuiltinSignatures.nistp521,
                    BuiltinSignatures.ed25519,
                    BuiltinSignatures.rsa,
                    BuiltinSignatures.dsa
            ));

    protected org.xbib.io.sshd.common.Factory<T> factory;
    protected List<NamedFactory<KeyExchange>> keyExchangeFactories;
    protected List<NamedFactory<Cipher>> cipherFactories;
    protected List<NamedFactory<Compression>> compressionFactories;
    protected List<NamedFactory<Mac>> macFactories;
    protected List<NamedFactory<Signature>> signatureFactories;
    protected org.xbib.io.sshd.common.Factory<Random> randomFactory;
    protected List<NamedFactory<Channel>> channelFactories;
    protected FileSystemFactory fileSystemFactory;
    protected TcpipForwarderFactory tcpipForwarderFactory;
    protected List<RequestHandler<ConnectionService>> globalRequestHandlers;
    protected ForwardingFilter forwardingFilter;

    public BaseBuilder() {
        super();
    }

    /**
     * @param ignoreUnsupported If {@code true} then all the default
     *                          ciphers are included, regardless of whether they are currently
     *                          supported by the JCE. Otherwise, only the supported ones out of the
     *                          list are included
     * @return A {@link List} of the default {@link NamedFactory}
     * instances of the {@link Cipher}s according to the preference
     * order defined by {@link #DEFAULT_CIPHERS_PREFERENCE}.
     * <B>Note:</B> the list may be filtered to exclude unsupported JCE
     * ciphers according to the <tt>ignoreUnsupported</tt> parameter
     * @see BuiltinCiphers#isSupported()
     */
    public static List<NamedFactory<Cipher>> setUpDefaultCiphers(boolean ignoreUnsupported) {
        return NamedFactory.setUpBuiltinFactories(ignoreUnsupported, DEFAULT_CIPHERS_PREFERENCE);
    }

    /**
     * @param ignoreUnsupported If {@code true} all the available built-in
     *                          {@link Mac} factories are added, otherwise only those that are supported
     *                          by the current JDK setup
     * @return A {@link List} of the default {@link NamedFactory}
     * instances of the {@link Mac}s according to the preference
     * order defined by {@link #DEFAULT_MAC_PREFERENCE}.
     * <B>Note:</B> the list may be filtered to exclude unsupported JCE
     * MACs according to the <tt>ignoreUnsupported</tt> parameter
     * @see BuiltinMacs#isSupported()
     */
    public static List<NamedFactory<Mac>> setUpDefaultMacs(boolean ignoreUnsupported) {
        return NamedFactory.setUpBuiltinFactories(ignoreUnsupported, DEFAULT_MAC_PREFERENCE);
    }

    /**
     * @param ignoreUnsupported If {@code true} all the available built-in
     *                          {@link Signature} factories are added, otherwise only those that are supported
     *                          by the current JDK setup
     * @return A {@link List} of the default {@link NamedFactory}
     * instances of the {@link Signature}s according to the preference
     * order defined by {@link #DEFAULT_SIGNATURE_PREFERENCE}.
     * <B>Note:</B> the list may be filtered to exclude unsupported JCE
     * signatures according to the <tt>ignoreUnsupported</tt> parameter
     * @see BuiltinSignatures#isSupported()
     */
    public static List<NamedFactory<Signature>> setUpDefaultSignatures(boolean ignoreUnsupported) {
        return NamedFactory.setUpBuiltinFactories(ignoreUnsupported, DEFAULT_SIGNATURE_PREFERENCE);
    }

    protected S fillWithDefaultValues() {
        if (signatureFactories == null) {
            signatureFactories = setUpDefaultSignatures(false);
        }

        if (randomFactory == null) {
            randomFactory = new SingletonRandomFactory(SecurityUtils.getRandomFactory());
        }

        if (cipherFactories == null) {
            cipherFactories = setUpDefaultCiphers(false);
        }

        if (macFactories == null) {
            macFactories = setUpDefaultMacs(false);
        }

        if (fileSystemFactory == null) {
            fileSystemFactory = DEFAULT_FILE_SYSTEM_FACTORY;
        }

        if (forwardingFilter == null) {
            forwardingFilter = DEFAULT_FORWARDING_FILTER;
        }

        if (tcpipForwarderFactory == null) {
            tcpipForwarderFactory = DEFAULT_FORWARDER_FACTORY;
        }

        return me();
    }

    public S keyExchangeFactories(List<NamedFactory<KeyExchange>> keyExchangeFactories) {
        this.keyExchangeFactories = keyExchangeFactories;
        return me();
    }

    public S signatureFactories(List<NamedFactory<Signature>> signatureFactories) {
        this.signatureFactories = signatureFactories;
        return me();
    }

    public S randomFactory(org.xbib.io.sshd.common.Factory<Random> randomFactory) {
        this.randomFactory = randomFactory;
        return me();
    }

    public S cipherFactories(List<NamedFactory<Cipher>> cipherFactories) {
        this.cipherFactories = cipherFactories;
        return me();
    }

    public S compressionFactories(List<NamedFactory<Compression>> compressionFactories) {
        this.compressionFactories = compressionFactories;
        return me();
    }

    public S macFactories(List<NamedFactory<Mac>> macFactories) {
        this.macFactories = macFactories;
        return me();
    }

    public S channelFactories(List<NamedFactory<Channel>> channelFactories) {
        this.channelFactories = channelFactories;
        return me();
    }

    public S fileSystemFactory(FileSystemFactory fileSystemFactory) {
        this.fileSystemFactory = fileSystemFactory;
        return me();
    }

    public S forwardingFilter(ForwardingFilter filter) {
        this.forwardingFilter = filter;
        return me();
    }

    public S tcpipForwarderFactory(TcpipForwarderFactory tcpipForwarderFactory) {
        this.tcpipForwarderFactory = tcpipForwarderFactory;
        return me();
    }

    public S globalRequestHandlers(List<RequestHandler<ConnectionService>> globalRequestHandlers) {
        this.globalRequestHandlers = globalRequestHandlers;
        return me();
    }

    public S factory(Factory<T> factory) {
        this.factory = factory;
        return me();
    }

    public T build(final boolean isFillWithDefaultValues) {
        if (isFillWithDefaultValues) {
            fillWithDefaultValues();
        }

        T ssh = factory.create();

        ssh.setKeyExchangeFactories(keyExchangeFactories);
        ssh.setSignatureFactories(signatureFactories);
        ssh.setRandomFactory(randomFactory);
        ssh.setCipherFactories(cipherFactories);
        ssh.setCompressionFactories(compressionFactories);
        ssh.setMacFactories(macFactories);
        ssh.setChannelFactories(channelFactories);
        ssh.setFileSystemFactory(fileSystemFactory);
        ssh.setTcpipForwardingFilter(forwardingFilter);
        ssh.setTcpipForwarderFactory(tcpipForwarderFactory);
        ssh.setGlobalRequestHandlers(globalRequestHandlers);
        return ssh;
    }

    @Override
    public T build() {
        return build(true);
    }

    @SuppressWarnings("unchecked")
    protected S me() {
        return (S) this;
    }
}

package org.xbib.io.sshd.server;

import org.xbib.io.sshd.common.Closeable;
import org.xbib.io.sshd.common.Factory;
import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.PropertyResolverUtils;
import org.xbib.io.sshd.common.ServiceFactory;
import org.xbib.io.sshd.common.config.SshConfigFileReader;
import org.xbib.io.sshd.common.config.keys.BuiltinIdentities;
import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.forward.ForwardingFilter;
import org.xbib.io.sshd.common.helpers.AbstractFactoryManager;
import org.xbib.io.sshd.common.io.IoAcceptor;
import org.xbib.io.sshd.common.io.IoServiceFactory;
import org.xbib.io.sshd.common.io.IoSession;
import org.xbib.io.sshd.common.io.nio2.Nio2ServiceFactory;
import org.xbib.io.sshd.common.keyprovider.KeyPairProvider;
import org.xbib.io.sshd.common.keyprovider.MappedKeyPairProvider;
import org.xbib.io.sshd.common.session.helpers.AbstractSession;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.security.SecurityUtils;
import org.xbib.io.sshd.server.auth.UserAuth;
import org.xbib.io.sshd.server.auth.hostbased.HostBasedAuthenticator;
import org.xbib.io.sshd.server.auth.keyboard.KeyboardInteractiveAuthenticator;
import org.xbib.io.sshd.server.auth.password.PasswordAuthenticator;
import org.xbib.io.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.xbib.io.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.xbib.io.sshd.server.forward.AcceptAllForwardingFilter;
import org.xbib.io.sshd.server.keyprovider.AbstractGeneratorHostKeyProvider;
import org.xbib.io.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.xbib.io.sshd.server.scp.ScpCommandFactory;
import org.xbib.io.sshd.server.session.ServerConnectionServiceFactory;
import org.xbib.io.sshd.server.session.ServerProxyAcceptor;
import org.xbib.io.sshd.server.session.ServerUserAuthServiceFactory;
import org.xbib.io.sshd.server.session.SessionFactory;
import org.xbib.io.sshd.server.shell.InteractiveProcessShellFactory;
import org.xbib.io.sshd.server.shell.ProcessShellFactory;
import org.xbib.io.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.xbib.io.sshd.server.util.security.bouncycastle.BouncyCastleGeneratorHostKeyProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The SshServer class is the main entry point for the server side of the SSH protocol.
 * The SshServer has to be configured before being started.  Such configuration can be
 * done either using a dependency injection mechanism (such as the Spring framework)
 * or programmatically. Basic setup is usually done using the {@link #setUpDefaultServer()}
 * method, which will known ciphers, macs, channels, etc...
 * Besides this basic setup, a few things have to be manually configured such as the
 * port number, {@link Factory}, the {@link KeyPairProvider}
 * and the {@link PasswordAuthenticator}.
 * Some properties can also be configured using the {@link PropertyResolverUtils}
 * {@code updateProperty} methods.
 * Once the SshServer instance has been configured, it can be started using the
 * {@link #start()} method and stopped using the {@link #stop()} method.
 */
public class SshServer extends AbstractFactoryManager implements ServerFactoryManager, Closeable {

    public static final Factory<SshServer> DEFAULT_SSH_SERVER_FACTORY = SshServer::new;

    public static final List<ServiceFactory> DEFAULT_SERVICE_FACTORIES =
            Collections.unmodifiableList(Arrays.asList(
                    ServerUserAuthServiceFactory.INSTANCE,
                    ServerConnectionServiceFactory.INSTANCE
            ));


    protected IoAcceptor acceptor;
    protected String host;
    protected int port;

    private ServerProxyAcceptor proxyAcceptor;
    private Factory<Command> shellFactory;
    private SessionFactory sessionFactory;
    private CommandFactory commandFactory;
    private List<NamedFactory<Command>> subsystemFactories;
    private List<NamedFactory<UserAuth>> userAuthFactories;
    private PasswordAuthenticator passwordAuthenticator;
    private PublickeyAuthenticator publickeyAuthenticator;
    private KeyboardInteractiveAuthenticator interactiveAuthenticator;
    private HostBasedAuthenticator hostBasedAuthenticator;

    public SshServer() {
        super();
    }

    public static SshServer setUpDefaultServer() {
        return ServerBuilder.builder().build();
    }

    public static KeyPairProvider setupServerKeys(SshServer sshd, String hostKeyType, int hostKeySize, Collection<String> keyFiles) throws Exception {
        if (GenericUtils.isEmpty(keyFiles)) {
            AbstractGeneratorHostKeyProvider hostKeyProvider;
            Path hostKeyFile;
            if (SecurityUtils.isBouncyCastleRegistered()) {
                hostKeyFile = new File("key.pem").toPath();
                hostKeyProvider = createGeneratorHostKeyProvider(hostKeyFile);
            } else {
                hostKeyFile = new File("key.ser").toPath();
                hostKeyProvider = new SimpleGeneratorHostKeyProvider(hostKeyFile);
            }
            hostKeyProvider.setAlgorithm(hostKeyType);
            if (hostKeySize != 0) {
                hostKeyProvider.setKeySize(hostKeySize);
            }

            List<KeyPair> keys = ValidateUtils.checkNotNullAndNotEmpty(hostKeyProvider.loadKeys(),
                    "Failed to load keys from %s", hostKeyFile);
            KeyPair kp = keys.get(0);
            PublicKey pubKey = kp.getPublic();
            String keyAlgorithm = pubKey.getAlgorithm();
            if (BuiltinIdentities.Constants.ECDSA.equalsIgnoreCase(keyAlgorithm)) {
                keyAlgorithm = KeyUtils.EC_ALGORITHM;
            } else if (BuiltinIdentities.Constants.ED25519.equals(keyAlgorithm)) {
                keyAlgorithm = SecurityUtils.EDDSA;
                // TODO change the hostKeyProvider to one that supports read/write of EDDSA keys - see SSHD-703
            }

            // force re-generation of host key if not same algorithm
            if (!Objects.equals(keyAlgorithm, hostKeyType)) {
                Files.deleteIfExists(hostKeyFile);
                hostKeyProvider.clearLoadedKeys();
            }

            return hostKeyProvider;
        } else {
            List<KeyPair> pairs = new ArrayList<>(keyFiles.size());
            for (String keyFilePath : keyFiles) {
                Path path = Paths.get(keyFilePath);
                try (InputStream inputStream = Files.newInputStream(path)) {
                    KeyPair kp = SecurityUtils.loadKeyPairIdentity(keyFilePath, inputStream, null);
                    pairs.add(kp);
                } catch (Exception e) {
                    System.err.append("Failed (" + e.getClass().getSimpleName() + ")"
                            + " to load host key file=" + keyFilePath
                            + ": " + e.getMessage());
                    throw e;
                }
            }

            return new MappedKeyPairProvider(pairs);
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 8000;
        String provider;
        boolean error = false;
        String hostKeyType = AbstractGeneratorHostKeyProvider.DEFAULT_ALGORITHM;
        int hostKeySize = 0;
        Collection<String> keyFiles = null;
        Map<String, String> options = new LinkedHashMap<>();

        int numArgs = GenericUtils.length(args);
        for (int i = 0; i < numArgs; i++) {
            String argName = args[i];
            if ("-p".equals(argName)) {
                if (i + 1 >= numArgs) {
                    System.err.println("option requires an argument: " + argName);
                    error = true;
                    break;
                }
                port = Integer.parseInt(args[++i]);
            } else if ("-key-type".equals(argName)) {
                if (i + 1 >= numArgs) {
                    System.err.println("option requires an argument: " + argName);
                    error = true;
                    break;
                }

                if (keyFiles != null) {
                    System.err.println("option conflicts with -key-file: " + argName);
                    error = true;
                    break;
                }
                hostKeyType = args[++i].toUpperCase();
            } else if ("-key-size".equals(argName)) {
                if (i + 1 >= numArgs) {
                    System.err.println("option requires an argument: " + argName);
                    error = true;
                    break;
                }

                if (keyFiles != null) {
                    System.err.println("option conflicts with -key-file: " + argName);
                    error = true;
                    break;
                }

                hostKeySize = Integer.parseInt(args[++i]);
            } else if ("-key-file".equals(argName)) {
                if (i + 1 >= numArgs) {
                    System.err.println("option requires an argument: " + argName);
                    error = true;
                    break;
                }

                String keyFilePath = args[++i];
                if (keyFiles == null) {
                    keyFiles = new LinkedList<>();
                }
                keyFiles.add(keyFilePath);
            } else if ("-io".equals(argName)) {
                if (i + 1 >= numArgs) {
                    System.err.println("option requires an argument: " + argName);
                    error = true;
                    break;
                }
                provider = args[++i];
                if ("nio2".endsWith(provider)) {
                    System.setProperty(IoServiceFactory.class.getName(), Nio2ServiceFactory.class.getName());
                } else {
                    System.err.println("provider should be nio2: " + argName);
                    error = true;
                    break;
                }
            } else if ("-o".equals(argName)) {
                if (i + 1 >= numArgs) {
                    System.err.println("option requires and argument: " + argName);
                    error = true;
                    break;
                }
                String opt = args[++i];
                int idx = opt.indexOf('=');
                if (idx <= 0) {
                    System.err.println("bad syntax for option: " + opt);
                    error = true;
                    break;
                }
                options.put(opt.substring(0, idx), opt.substring(idx + 1));
            } else if (argName.startsWith("-")) {
                System.err.println("illegal option: " + argName);
                error = true;
                break;
            } else {
                System.err.println("extra argument: " + argName);
                error = true;
                break;
            }
        }
        if (error) {
            System.err.println("usage: sshd [-p port] [-io mina|nio2] [-key-type RSA|DSA|EC] [-key-size NNNN] [-key-file <path>] [-o option=value]");
            System.exit(-1);
        }

        System.err.println("Starting SSHD on port " + port);

        SshServer sshd = SshServer.setUpDefaultServer();
        Map<String, Object> props = sshd.getProperties();
        props.putAll(options);

        KeyPairProvider hostKeyProvider = setupServerKeys(sshd, hostKeyType, hostKeySize, keyFiles);
        sshd.setKeyPairProvider(hostKeyProvider);
        // Should come AFTER key pair provider setup so auto-welcome can be generated if needed
        setupServerBanner(sshd, options);
        sshd.setPort(port);

        sshd.setShellFactory(InteractiveProcessShellFactory.INSTANCE);
        sshd.setPasswordAuthenticator((username, password, session) -> Objects.equals(username, password));
        sshd.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);
        sshd.setTcpipForwardingFilter(AcceptAllForwardingFilter.INSTANCE);
        sshd.setCommandFactory(new ScpCommandFactory.Builder().withDelegate(
                command -> new ProcessShellFactory(GenericUtils.split(command, ' ')).create()
        ).build());
        sshd.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
        sshd.start();

        Thread.sleep(Long.MAX_VALUE);
    }

    public static Object setupServerBanner(ServerFactoryManager server, Map<String, ?> options) throws Exception {
        String bannerOption = GenericUtils.isEmpty(options)
                ? null
                : Objects.toString(options.remove(SshConfigFileReader.BANNER_CONFIG_PROP), null);
        if (GenericUtils.isEmpty(bannerOption)) {
            bannerOption = GenericUtils.isEmpty(options)
                    ? null
                    : Objects.toString(options.remove(SshConfigFileReader.VISUAL_HOST_KEY), null);
            if (SshConfigFileReader.parseBooleanValue(bannerOption)) {
                bannerOption = AUTO_WELCOME_BANNER_VALUE;
            }
        }

        Object banner;
        if (GenericUtils.isNotEmpty(bannerOption)) {
            if ("none".equals(bannerOption)) {
                return null;
            }

            if (AUTO_WELCOME_BANNER_VALUE.equalsIgnoreCase(bannerOption)) {
                banner = bannerOption;
            } else {
                banner = Paths.get(bannerOption);
            }
        } else {
            banner = "Welcome to SSHD\n";
        }

        PropertyResolverUtils.updateProperty(server, WELCOME_BANNER, banner);
        return banner;
    }

    public static BouncyCastleGeneratorHostKeyProvider createGeneratorHostKeyProvider(Path path) {
        ValidateUtils.checkTrue(SecurityUtils.isBouncyCastleRegistered(), "BouncyCastle not registered");
        return new BouncyCastleGeneratorHostKeyProvider(path);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    /**
     * Configure the port number to use for this SSH server.
     *
     * @param port the port number for this SSH server
     */
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public List<NamedFactory<UserAuth>> getUserAuthFactories() {
        return userAuthFactories;
    }

    @Override
    public void setUserAuthFactories(List<NamedFactory<UserAuth>> userAuthFactories) {
        this.userAuthFactories = userAuthFactories;
    }

    @Override
    public Factory<Command> getShellFactory() {
        return shellFactory;
    }

    public void setShellFactory(Factory<Command> shellFactory) {
        this.shellFactory = shellFactory;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public ServerProxyAcceptor getServerProxyAcceptor() {
        return proxyAcceptor;
    }

    @Override
    public void setServerProxyAcceptor(ServerProxyAcceptor proxyAcceptor) {
        this.proxyAcceptor = proxyAcceptor;
    }

    @Override
    public CommandFactory getCommandFactory() {
        return commandFactory;
    }

    public void setCommandFactory(CommandFactory commandFactory) {
        this.commandFactory = commandFactory;
    }

    @Override
    public List<NamedFactory<Command>> getSubsystemFactories() {
        return subsystemFactories;
    }

    public void setSubsystemFactories(List<NamedFactory<Command>> subsystemFactories) {
        this.subsystemFactories = subsystemFactories;
    }

    @Override
    public PasswordAuthenticator getPasswordAuthenticator() {
        return passwordAuthenticator;
    }

    @Override
    public void setPasswordAuthenticator(PasswordAuthenticator passwordAuthenticator) {
        this.passwordAuthenticator = passwordAuthenticator;
    }

    @Override
    public PublickeyAuthenticator getPublickeyAuthenticator() {
        return publickeyAuthenticator;
    }

    @Override
    public void setPublickeyAuthenticator(PublickeyAuthenticator publickeyAuthenticator) {
        this.publickeyAuthenticator = publickeyAuthenticator;
    }

    @Override
    public KeyboardInteractiveAuthenticator getKeyboardInteractiveAuthenticator() {
        return interactiveAuthenticator;
    }

    @Override
    public void setKeyboardInteractiveAuthenticator(KeyboardInteractiveAuthenticator interactiveAuthenticator) {
        this.interactiveAuthenticator = interactiveAuthenticator;
    }

    @Override
    public HostBasedAuthenticator getHostBasedAuthenticator() {
        return hostBasedAuthenticator;
    }

    @Override
    public void setHostBasedAuthenticator(HostBasedAuthenticator hostBasedAuthenticator) {
        this.hostBasedAuthenticator = hostBasedAuthenticator;
    }

    @Override
    public void setTcpipForwardingFilter(ForwardingFilter forwardingFilter) {
        this.tcpipForwardingFilter = forwardingFilter;
    }

    @Override
    protected void checkConfig() {
        super.checkConfig();
        ValidateUtils.checkTrue(getPort() >= 0 /* zero means not set yet */, "Bad port number: %d", Integer.valueOf(getPort()));
        List<NamedFactory<UserAuth>> authFactories = ServerAuthenticationManager.resolveUserAuthFactories(this);
        setUserAuthFactories(ValidateUtils.checkNotNullAndNotEmpty(authFactories, "UserAuthFactories not set"));
        ValidateUtils.checkNotNullAndNotEmpty(getChannelFactories(), "ChannelFactories not set");
        Objects.requireNonNull(getKeyPairProvider(), "HostKeyProvider not set");
        Objects.requireNonNull(getFileSystemFactory(), "FileSystemFactory not set");

        if (GenericUtils.isEmpty(getServiceFactories())) {
            setServiceFactories(DEFAULT_SERVICE_FACTORIES);
        }
    }

    /**
     * Start the SSH server and accept incoming exceptions on the configured port.
     *
     * @throws IOException If failed to start
     */
    public void start() throws IOException {
        checkConfig();
        if (sessionFactory == null) {
            sessionFactory = createSessionFactory();
        }
        acceptor = createAcceptor();

        setupSessionTimeout(sessionFactory);

        String hostsList = getHost();
        if (!GenericUtils.isEmpty(hostsList)) {
            String[] hosts = GenericUtils.split(hostsList, ',');
            for (String host : hosts) {
                InetAddress[] inetAddresses = InetAddress.getAllByName(host);
                for (InetAddress inetAddress : inetAddresses) {
                    acceptor.bind(new InetSocketAddress(inetAddress, port));
                    if (port == 0) {
                        port = ((InetSocketAddress) acceptor.getBoundAddresses().iterator().next()).getPort();
                        log.info("start() listen on auto-allocated port=" + port);
                    }
                }
            }
        } else {
            acceptor.bind(new InetSocketAddress(port));
            if (port == 0) {
                port = ((InetSocketAddress) acceptor.getBoundAddresses().iterator().next()).getPort();
                log.info("start() listen on auto-allocated port=" + port);
            }
        }
    }

    /**
     * Stop the SSH server.  This method will block until all resources are actually disposed.
     *
     * @throws IOException if stopping failed somehow
     */
    public void stop() throws IOException {
        stop(false);
    }

    public void stop(boolean immediately) throws IOException {
        long maxWait = immediately ? this.getLongProperty(STOP_WAIT_TIME, DEFAULT_STOP_WAIT_TIME) : Long.MAX_VALUE;
        boolean successful = close(immediately).await(maxWait);
        if (!successful) {
            throw new SocketTimeoutException("Failed to receive closure confirmation within " + maxWait + " millis");
        }
    }

    public void open() throws IOException {
        start();
    }

    @Override
    protected Closeable getInnerCloseable() {
        return builder()
                .run(() -> removeSessionTimeout(sessionFactory))
                .sequential(acceptor, ioServiceFactory)
                .run(() -> {
                    acceptor = null;
                    ioServiceFactory = null;
                    if (shutdownExecutor && (executor != null) && (!executor.isShutdown())) {
                        try {
                            executor.shutdownNow();
                        } finally {
                            executor = null;
                        }
                    }
                })
                .build();
    }

    /**
     * Obtain the list of active sessions.
     *
     * @return A {@link List} of the currently active session
     */
    public List<AbstractSession> getActiveSessions() {
        List<AbstractSession> sessions = new ArrayList<>();
        for (IoSession ioSession : acceptor.getManagedSessions().values()) {
            AbstractSession session = AbstractSession.getSession(ioSession, true);
            if (session != null) {
                sessions.add(session);
            }
        }
        return sessions;
    }

    protected IoAcceptor createAcceptor() {
        IoServiceFactory ioFactory = getIoServiceFactory();
        SessionFactory sessFactory = getSessionFactory();
        return ioFactory.createAcceptor(sessFactory);
    }

    protected SessionFactory createSessionFactory() {
        return new SessionFactory(this);
    }

    @Override
    public String toString() {
        return "SshServer[" + Integer.toHexString(hashCode()) + "]";
    }

}

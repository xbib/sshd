package org.xbib.io.sshd.server.subsystem.sftp;

import org.xbib.io.sshd.common.Factory;
import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.OptionalFeature;
import org.xbib.io.sshd.common.PropertyResolver;
import org.xbib.io.sshd.common.PropertyResolverUtils;
import org.xbib.io.sshd.common.config.VersionProperties;
import org.xbib.io.sshd.common.digest.BuiltinDigests;
import org.xbib.io.sshd.common.digest.Digest;
import org.xbib.io.sshd.common.digest.DigestFactory;
import org.xbib.io.sshd.common.file.FileSystemAware;
import org.xbib.io.sshd.common.random.Random;
import org.xbib.io.sshd.common.subsystem.sftp.SftpConstants;
import org.xbib.io.sshd.common.subsystem.sftp.SftpException;
import org.xbib.io.sshd.common.subsystem.sftp.SftpHelper;
import org.xbib.io.sshd.common.subsystem.sftp.extensions.AclSupportedParser;
import org.xbib.io.sshd.common.subsystem.sftp.extensions.SpaceAvailableExtensionInfo;
import org.xbib.io.sshd.common.subsystem.sftp.extensions.openssh.AbstractOpenSSHExtensionParser.OpenSSHExtension;
import org.xbib.io.sshd.common.subsystem.sftp.extensions.openssh.FsyncExtensionParser;
import org.xbib.io.sshd.common.subsystem.sftp.extensions.openssh.HardLinkExtensionParser;
import org.xbib.io.sshd.common.util.EventListenerUtils;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.NumberUtils;
import org.xbib.io.sshd.common.util.OsUtils;
import org.xbib.io.sshd.common.util.Pair;
import org.xbib.io.sshd.common.util.SelectorUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.BufferUtils;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;
import org.xbib.io.sshd.common.util.io.FileInfoExtractor;
import org.xbib.io.sshd.common.util.io.IoUtils;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;
import org.xbib.io.sshd.common.util.threads.ThreadUtils;
import org.xbib.io.sshd.server.Command;
import org.xbib.io.sshd.server.Environment;
import org.xbib.io.sshd.server.ExitCallback;
import org.xbib.io.sshd.server.SessionAware;
import org.xbib.io.sshd.server.session.ServerSession;
import org.xbib.io.sshd.server.session.ServerSessionHolder;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.nio.ByteBuffer;
import java.nio.channels.OverlappingFileLockException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * SFTP subsystem.
 */
public class SftpSubsystem
        extends AbstractLoggingBean
        implements Command, Runnable, SessionAware, FileSystemAware, ServerSessionHolder, SftpEventListenerManager {

    /**
     * Properties key for the maximum of available open handles per session.
     */
    public static final String MAX_OPEN_HANDLES_PER_SESSION = "max-open-handles-per-session";
    public static final int DEFAULT_MAX_OPEN_HANDLES = Integer.MAX_VALUE;

    /**
     * Size in bytes of the opaque handle value
     *
     * @see #DEFAULT_FILE_HANDLE_SIZE
     */
    public static final String FILE_HANDLE_SIZE = "sftp-handle-size";
    public static final int MIN_FILE_HANDLE_SIZE = 4;  // ~uint32
    public static final int DEFAULT_FILE_HANDLE_SIZE = 16;
    public static final int MAX_FILE_HANDLE_SIZE = 64;  // ~sha512

    /**
     * Max. rounds to attempt to create a unique file handle - if all handles
     * already in use after these many rounds, then an exception is thrown
     *
     * @see #generateFileHandle(Path)
     * @see #DEFAULT_FILE_HANDLE_ROUNDS
     */
    public static final String MAX_FILE_HANDLE_RAND_ROUNDS = "sftp-handle-rand-max-rounds";
    public static final int MIN_FILE_HANDLE_ROUNDS = 1;
    public static final int DEFAULT_FILE_HANDLE_ROUNDS = MIN_FILE_HANDLE_SIZE;
    public static final int MAX_FILE_HANDLE_ROUNDS = MAX_FILE_HANDLE_SIZE;

    /**
     * Force the use of a given sftp version
     */
    public static final String SFTP_VERSION = "sftp-version";

    public static final int LOWER_SFTP_IMPL = SftpConstants.SFTP_V3; // Working implementation from v3
    public static final int HIGHER_SFTP_IMPL = SftpConstants.SFTP_V6; //  .. up to and including
    public static final String ALL_SFTP_IMPL = IntStream.rangeClosed(LOWER_SFTP_IMPL, HIGHER_SFTP_IMPL)
            .mapToObj(Integer::toString)
            .collect(Collectors.joining(","));

    /**
     * Force the use of a max. packet length for {@link #doRead(Buffer, int)} protection
     * against malicious packets
     *
     * @see #DEFAULT_MAX_READDATA_PACKET_LENGTH
     */
    public static final String MAX_READDATA_PACKET_LENGTH_PROP = "sftp-max-readdata-packet-length";
    public static final int DEFAULT_MAX_READDATA_PACKET_LENGTH = 63 * 1024;

    /**
     * Maximum amount of data allocated for listing the contents of a directory
     * in any single invocation of {@link #doReadDir(Buffer, int)}
     *
     * @see #DEFAULT_MAX_READDIR_DATA_SIZE
     */
    public static final String MAX_READDIR_DATA_SIZE_PROP = "sftp-max-readdir-data-size";
    public static final int DEFAULT_MAX_READDIR_DATA_SIZE = 16 * 1024;

    /**
     * Allows controlling reports of which client extensions are supported
     * (and reported via &quot;support&quot; and &quot;support2&quot; server
     * extensions) as a comma-separate list of names. <B>Note:</B> requires
     * overriding the {@link #executeExtendedCommand(Buffer, int, String)}
     * command accordingly. If empty string is set then no server extensions
     * are reported
     *
     * @see #DEFAULT_SUPPORTED_CLIENT_EXTENSIONS
     */
    public static final String CLIENT_EXTENSIONS_PROP = "sftp-client-extensions";

    /**
     * The default reported supported client extensions
     */
    public static final Map<String, OptionalFeature> DEFAULT_SUPPORTED_CLIENT_EXTENSIONS =
            // TODO text-seek - see http://tools.ietf.org/wg/secsh/draft-ietf-secsh-filexfer/draft-ietf-secsh-filexfer-13.txt
            // TODO home-directory - see http://tools.ietf.org/wg/secsh/draft-ietf-secsh-filexfer/draft-ietf-secsh-filexfer-09.txt
            GenericUtils.<String, OptionalFeature>mapBuilder()
                    .put(SftpConstants.EXT_VERSION_SELECT, OptionalFeature.TRUE)
                    .put(SftpConstants.EXT_COPY_FILE, OptionalFeature.TRUE)
                    .put(SftpConstants.EXT_MD5_HASH, BuiltinDigests.md5)
                    .put(SftpConstants.EXT_MD5_HASH_HANDLE, BuiltinDigests.md5)
                    .put(SftpConstants.EXT_CHECK_FILE_HANDLE, OptionalFeature.any(BuiltinDigests.VALUES))
                    .put(SftpConstants.EXT_CHECK_FILE_NAME, OptionalFeature.any(BuiltinDigests.VALUES))
                    .put(SftpConstants.EXT_COPY_DATA, OptionalFeature.TRUE)
                    .put(SftpConstants.EXT_SPACE_AVAILABLE, OptionalFeature.TRUE)
                    .immutable();

    /**
     * Comma-separated list of which {@code OpenSSH} extensions are reported and
     * what version is reported for each - format: {@code name=version}. If empty
     * value set, then no such extensions are reported. Otherwise, the
     * {@link #DEFAULT_OPEN_SSH_EXTENSIONS} are used
     */
    public static final String OPENSSH_EXTENSIONS_PROP = "sftp-openssh-extensions";
    public static final List<OpenSSHExtension> DEFAULT_OPEN_SSH_EXTENSIONS =
            Collections.unmodifiableList(
                    Arrays.asList(
                            new OpenSSHExtension(FsyncExtensionParser.NAME, "1"),
                            new OpenSSHExtension(HardLinkExtensionParser.NAME, "1")
                    ));

    public static final List<String> DEFAULT_OPEN_SSH_EXTENSIONS_NAMES =
            Collections.unmodifiableList(NamedResource.getNameList(DEFAULT_OPEN_SSH_EXTENSIONS));

    public static final List<String> DEFAULT_UNIX_VIEW = Collections.singletonList("unix:*");

    /**
     * Comma separate list of {@code SSH_ACL_CAP_xxx} names - where name can be without
     * the prefix. If not defined then {@link #DEFAULT_ACL_SUPPORTED_MASK} is used
     */
    public static final String ACL_SUPPORTED_MASK_PROP = "sftp-acl-supported-mask";
    public static final Set<Integer> DEFAULT_ACL_SUPPORTED_MASK =
            Collections.unmodifiableSet(
                    new HashSet<>(Arrays.asList(
                            SftpConstants.SSH_ACL_CAP_ALLOW,
                            SftpConstants.SSH_ACL_CAP_DENY,
                            SftpConstants.SSH_ACL_CAP_AUDIT,
                            SftpConstants.SSH_ACL_CAP_ALARM)));

    /**
     * Property that can be used to set the reported NL value.
     * If not set, then {@link IoUtils#EOL} is used
     */
    public static final String NEWLINE_VALUE = "sftp-newline";

    /**
     * A {@link Map} of {@link FileInfoExtractor}s to be used to complete
     * attributes that are deemed important enough to warrant an extra
     * effort if not accessible via the file system attributes views
     */
    public static final Map<String, FileInfoExtractor<?>> FILEATTRS_RESOLVERS =
            GenericUtils.<String, FileInfoExtractor<?>>mapBuilder(String.CASE_INSENSITIVE_ORDER)
                    .put("isRegularFile", FileInfoExtractor.ISREG)
                    .put("isDirectory", FileInfoExtractor.ISDIR)
                    .put("isSymbolicLink", FileInfoExtractor.ISSYMLINK)
                    .put("permissions", FileInfoExtractor.PERMISSIONS)
                    .put("size", FileInfoExtractor.SIZE)
                    .put("lastModifiedTime", FileInfoExtractor.LASTMODIFIED)
                    .immutable();

    /**
     * Whether to automatically follow symbolic links when resolving paths
     *
     * @see #DEFAULT_AUTO_FOLLOW_LINKS
     */
    public static final String AUTO_FOLLOW_LINKS = "sftp-auto-follow-links";

    /**
     * Default value of {@value #AUTO_FOLLOW_LINKS}
     */
    public static final boolean DEFAULT_AUTO_FOLLOW_LINKS = true;
    protected final Map<String, byte[]> extensions = new HashMap<>();
    protected final Map<String, Handle> handles = new HashMap<>();
    protected final UnsupportedAttributePolicy unsupportedAttributePolicy;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Collection<SftpEventListener> sftpEventListeners = new CopyOnWriteArraySet<>();
    private final SftpEventListener sftpEventListenerProxy;
    private final SftpFileSystemAccessor fileSystemAccessor;
    protected ExitCallback callback;
    protected InputStream in;
    protected OutputStream out;
    protected OutputStream err;
    protected Environment env;
    protected Random randomizer;
    protected int fileHandleSize = DEFAULT_FILE_HANDLE_SIZE;
    protected int maxFileHandleRounds = DEFAULT_FILE_HANDLE_ROUNDS;
    protected ExecutorService executors;
    protected boolean shutdownExecutor;
    protected Future<?> pendingFuture;
    protected byte[] workBuf = new byte[Math.max(DEFAULT_FILE_HANDLE_SIZE, Integer.BYTES)];
    protected FileSystem fileSystem = FileSystems.getDefault();
    protected Path defaultDir = fileSystem.getPath(System.getProperty("user.dir"));
    protected long requestsCount;
    protected int version;
    private ServerSession serverSession;

    /**
     * @param executorService The {@link ExecutorService} to be used by
     *                        the {@link SftpSubsystem} command when starting execution. If
     *                        {@code null} then a single-threaded ad-hoc service is used.
     * @param shutdownOnExit  If {@code true} the {@link ExecutorService#shutdownNow()}
     *                        will be called when subsystem terminates - unless it is the ad-hoc
     *                        service, which will be shutdown regardless
     * @param policy          The {@link UnsupportedAttributePolicy} to use if failed to access
     *                        some local file attributes
     * @param accessor        The {@link SftpFileSystemAccessor} to use for opening files and directories
     * @see ThreadUtils#newSingleThreadExecutor(String)
     */
    public SftpSubsystem(ExecutorService executorService, boolean shutdownOnExit, UnsupportedAttributePolicy policy, SftpFileSystemAccessor accessor) {
        if (executorService == null) {
            executors = ThreadUtils.newSingleThreadExecutor(getClass().getSimpleName());
            shutdownExecutor = true;    // we always close the ad-hoc executor service
        } else {
            executors = executorService;
            shutdownExecutor = shutdownOnExit;
        }

        unsupportedAttributePolicy = Objects.requireNonNull(policy, "No policy provided");
        fileSystemAccessor = Objects.requireNonNull(accessor, "No accessor");
        sftpEventListenerProxy = EventListenerUtils.proxyWrapper(SftpEventListener.class, getClass().getClassLoader(), sftpEventListeners);
    }

    /**
     * Returns the most adequate sub-status for the provided exception
     *
     * @param t The thrown {@link Throwable}
     * @return The matching sub-status
     */
    public static int resolveSubstatus(Throwable t) {
        if ((t instanceof NoSuchFileException) || (t instanceof FileNotFoundException)) {
            return SftpConstants.SSH_FX_NO_SUCH_FILE;
        } else if (t instanceof InvalidHandleException) {
            return SftpConstants.SSH_FX_INVALID_HANDLE;
        } else if (t instanceof FileAlreadyExistsException) {
            return SftpConstants.SSH_FX_FILE_ALREADY_EXISTS;
        } else if (t instanceof DirectoryNotEmptyException) {
            return SftpConstants.SSH_FX_DIR_NOT_EMPTY;
        } else if (t instanceof NotDirectoryException) {
            return SftpConstants.SSH_FX_NOT_A_DIRECTORY;
        } else if (t instanceof AccessDeniedException) {
            return SftpConstants.SSH_FX_PERMISSION_DENIED;
        } else if (t instanceof EOFException) {
            return SftpConstants.SSH_FX_EOF;
        } else if (t instanceof OverlappingFileLockException) {
            return SftpConstants.SSH_FX_LOCK_CONFLICT;
        } else if (t instanceof UnsupportedOperationException) {
            return SftpConstants.SSH_FX_OP_UNSUPPORTED;
        } else if (t instanceof InvalidPathException) {
            return SftpConstants.SSH_FX_INVALID_FILENAME;
        } else if (t instanceof IllegalArgumentException) {
            return SftpConstants.SSH_FX_INVALID_PARAMETER;
        } else if (t instanceof UnsupportedOperationException) {
            return SftpConstants.SSH_FX_OP_UNSUPPORTED;
        } else if (t instanceof UserPrincipalNotFoundException) {
            return SftpConstants.SSH_FX_UNKNOWN_PRINCIPAL;
        } else if (t instanceof FileSystemLoopException) {
            return SftpConstants.SSH_FX_LINK_LOOP;
        } else if (t instanceof SftpException) {
            return ((SftpException) t).getStatus();
        } else {
            return SftpConstants.SSH_FX_FAILURE;
        }
    }

    public int getVersion() {
        return version;
    }

    public final UnsupportedAttributePolicy getUnsupportedAttributePolicy() {
        return unsupportedAttributePolicy;
    }

    public final SftpFileSystemAccessor getFileSystemAccessor() {
        return fileSystemAccessor;
    }

    @Override
    public SftpEventListener getSftpEventListenerProxy() {
        return sftpEventListenerProxy;
    }

    @Override
    public boolean addSftpEventListener(SftpEventListener listener) {
        return sftpEventListeners.add(SftpEventListener.validateListener(listener));
    }

    @Override
    public boolean removeSftpEventListener(SftpEventListener listener) {
        if (listener == null) {
            return false;
        }

        return sftpEventListeners.remove(SftpEventListener.validateListener(listener));
    }

    @Override
    public void setSession(ServerSession session) {
        this.serverSession = Objects.requireNonNull(session, "No session");

        FactoryManager manager = session.getFactoryManager();
        Factory<? extends Random> factory = manager.getRandomFactory();
        this.randomizer = factory.create();

        this.fileHandleSize = session.getIntProperty(FILE_HANDLE_SIZE, DEFAULT_FILE_HANDLE_SIZE);
        ValidateUtils.checkTrue(this.fileHandleSize >= MIN_FILE_HANDLE_SIZE, "File handle size too small: %d", this.fileHandleSize);
        ValidateUtils.checkTrue(this.fileHandleSize <= MAX_FILE_HANDLE_SIZE, "File handle size too big: %d", this.fileHandleSize);

        this.maxFileHandleRounds = session.getIntProperty(MAX_FILE_HANDLE_RAND_ROUNDS, DEFAULT_FILE_HANDLE_ROUNDS);
        ValidateUtils.checkTrue(this.maxFileHandleRounds >= MIN_FILE_HANDLE_ROUNDS, "File handle rounds too small: %d", this.maxFileHandleRounds);
        ValidateUtils.checkTrue(this.maxFileHandleRounds <= MAX_FILE_HANDLE_ROUNDS, "File handle rounds too big: %d", this.maxFileHandleRounds);

        if (workBuf.length < this.fileHandleSize) {
            workBuf = new byte[this.fileHandleSize];
        }
    }

    @Override
    public ServerSession getServerSession() {
        return serverSession;
    }

    @Override
    public void setFileSystem(FileSystem fileSystem) {
        if (fileSystem != this.fileSystem) {
            this.fileSystem = fileSystem;

            Iterable<Path> roots = Objects.requireNonNull(fileSystem.getRootDirectories(), "No root directories");
            Iterator<Path> available = Objects.requireNonNull(roots.iterator(), "No roots iterator");
            ValidateUtils.checkTrue(available.hasNext(), "No available root");
            this.defaultDir = available.next();
        }
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

    @Override
    public void setInputStream(InputStream in) {
        this.in = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    @Override
    public void start(Environment env) throws IOException {
        this.env = env;
        try {
            pendingFuture = executors.submit(this);
        } catch (RuntimeException e) {    // e.g., RejectedExecutionException
            throw new IOException(e);
        }
    }

    @Override
    public void run() {
        try {
            for (long count = 1L; ; count++) {
                int length = BufferUtils.readInt(in, workBuf, 0, workBuf.length);
                ValidateUtils.checkTrue(length >= (Integer.BYTES + 1 /* command */), "Bad length to read: %d", length);

                Buffer buffer = new ByteArrayBuffer(length + Integer.BYTES + Long.SIZE /* a bit extra */, false);
                buffer.putInt(length);
                for (int remainLen = length; remainLen > 0; ) {
                    int l = in.read(buffer.array(), buffer.wpos(), remainLen);
                    if (l < 0) {
                        throw new IllegalArgumentException("Premature EOF at buffer #" + count + " while read length=" + length + " and remain=" + remainLen);
                    }
                    buffer.wpos(buffer.wpos() + l);
                    remainLen -= l;
                }

                process(buffer);
            }
        } catch (Throwable t) {
            if ((!closed.get()) && (!(t instanceof EOFException))) { // Ignore
            }
        } finally {
            handles.forEach((id, handle) -> {
                try {
                    handle.close();
                } catch (IOException ioe) {
                }
            });

            callback.onExit(0);
        }
    }

    protected void process(Buffer buffer) throws IOException {
        int length = buffer.getInt();
        int type = buffer.getUByte();
        int id = buffer.getInt();

        switch (type) {
            case SftpConstants.SSH_FXP_INIT:
                doInit(buffer, id);
                break;
            case SftpConstants.SSH_FXP_OPEN:
                doOpen(buffer, id);
                break;
            case SftpConstants.SSH_FXP_CLOSE:
                doClose(buffer, id);
                break;
            case SftpConstants.SSH_FXP_READ:
                doRead(buffer, id);
                break;
            case SftpConstants.SSH_FXP_WRITE:
                doWrite(buffer, id);
                break;
            case SftpConstants.SSH_FXP_LSTAT:
                doLStat(buffer, id);
                break;
            case SftpConstants.SSH_FXP_FSTAT:
                doFStat(buffer, id);
                break;
            case SftpConstants.SSH_FXP_SETSTAT:
                doSetStat(buffer, id);
                break;
            case SftpConstants.SSH_FXP_FSETSTAT:
                doFSetStat(buffer, id);
                break;
            case SftpConstants.SSH_FXP_OPENDIR:
                doOpenDir(buffer, id);
                break;
            case SftpConstants.SSH_FXP_READDIR:
                doReadDir(buffer, id);
                break;
            case SftpConstants.SSH_FXP_REMOVE:
                doRemove(buffer, id);
                break;
            case SftpConstants.SSH_FXP_MKDIR:
                doMakeDirectory(buffer, id);
                break;
            case SftpConstants.SSH_FXP_RMDIR:
                doRemoveDirectory(buffer, id);
                break;
            case SftpConstants.SSH_FXP_REALPATH:
                doRealPath(buffer, id);
                break;
            case SftpConstants.SSH_FXP_STAT:
                doStat(buffer, id);
                break;
            case SftpConstants.SSH_FXP_RENAME:
                doRename(buffer, id);
                break;
            case SftpConstants.SSH_FXP_READLINK:
                doReadLink(buffer, id);
                break;
            case SftpConstants.SSH_FXP_SYMLINK:
                doSymLink(buffer, id);
                break;
            case SftpConstants.SSH_FXP_LINK:
                doLink(buffer, id);
                break;
            case SftpConstants.SSH_FXP_BLOCK:
                doBlock(buffer, id);
                break;
            case SftpConstants.SSH_FXP_UNBLOCK:
                doUnblock(buffer, id);
                break;
            case SftpConstants.SSH_FXP_EXTENDED:
                doExtended(buffer, id);
                break;
            default: {
                String name = SftpConstants.getCommandMessageName(type);
                sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_OP_UNSUPPORTED, "Command " + name + " is unsupported or not implemented");
            }
        }

        if (type != SftpConstants.SSH_FXP_INIT) {
            requestsCount++;
        }
    }

    protected void doExtended(Buffer buffer, int id) throws IOException {
        executeExtendedCommand(buffer, id, buffer.getString());
    }

    /**
     * @param buffer    The command {@link Buffer}
     * @param id        The request id
     * @param extension The extension name
     * @throws IOException If failed to execute the extension
     */
    protected void executeExtendedCommand(Buffer buffer, int id, String extension) throws IOException {
        switch (extension) {
            case SftpConstants.EXT_TEXT_SEEK:
                doTextSeek(buffer, id);
                break;
            case SftpConstants.EXT_VERSION_SELECT:
                doVersionSelect(buffer, id);
                break;
            case SftpConstants.EXT_COPY_FILE:
                doCopyFile(buffer, id);
                break;
            case SftpConstants.EXT_COPY_DATA:
                doCopyData(buffer, id);
                break;
            case SftpConstants.EXT_MD5_HASH:
            case SftpConstants.EXT_MD5_HASH_HANDLE:
                doMD5Hash(buffer, id, extension);
                break;
            case SftpConstants.EXT_CHECK_FILE_HANDLE:
            case SftpConstants.EXT_CHECK_FILE_NAME:
                doCheckFileHash(buffer, id, extension);
                break;
            case FsyncExtensionParser.NAME:
                doOpenSSHFsync(buffer, id);
                break;
            case SftpConstants.EXT_SPACE_AVAILABLE:
                doSpaceAvailable(buffer, id);
                break;
            case HardLinkExtensionParser.NAME:
                doOpenSSHHardLink(buffer, id);
                break;
            default:
                sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_OP_UNSUPPORTED, "Command SSH_FXP_EXTENDED(" + extension + ") is unsupported or not implemented");
                break;
        }
    }

    // see https://github.com/openssh/openssh-portable/blob/master/PROTOCOL section 10
    protected void doOpenSSHHardLink(Buffer buffer, int id) throws IOException {
        String srcFile = buffer.getString();
        String dstFile = buffer.getString();

        try {
            doOpenSSHHardLink(id, srcFile, dstFile);
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_OK, "");
    }

    protected void doOpenSSHHardLink(int id, String srcFile, String dstFile) throws IOException {
        createLink(id, srcFile, dstFile, false);
    }

    protected void doSpaceAvailable(Buffer buffer, int id) throws IOException {
        String path = buffer.getString();
        SpaceAvailableExtensionInfo info;
        try {
            info = doSpaceAvailable(id, path);
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        buffer.clear();
        buffer.putByte((byte) SftpConstants.SSH_FXP_EXTENDED_REPLY);
        buffer.putInt(id);
        SpaceAvailableExtensionInfo.encode(buffer, info);
        send(buffer);
    }

    protected SpaceAvailableExtensionInfo doSpaceAvailable(int id, String path) throws IOException {
        Path nrm = resolveNormalizedLocation(path);
        FileStore store = Files.getFileStore(nrm);
        return new SpaceAvailableExtensionInfo(store);
    }

    protected void doTextSeek(Buffer buffer, int id) throws IOException {
        String handle = buffer.getString();
        long line = buffer.getLong();
        try {
            // TODO : implement text-seek - see https://tools.ietf.org/html/draft-ietf-secsh-filexfer-03#section-6.3
            doTextSeek(id, handle, line);
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_OK, "");
    }

    protected void doTextSeek(int id, String handle, long line) throws IOException {
        Handle h = handles.get(handle);

        FileHandle fileHandle = validateHandle(handle, h, FileHandle.class);
        throw new UnsupportedOperationException("doTextSeek(" + fileHandle + ")");
    }

    // see https://github.com/openssh/openssh-portable/blob/master/PROTOCOL section 10
    protected void doOpenSSHFsync(Buffer buffer, int id) throws IOException {
        String handle = buffer.getString();
        try {
            doOpenSSHFsync(id, handle);
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_OK, "");
    }

    protected void doOpenSSHFsync(int id, String handle) throws IOException {
        Handle h = handles.get(handle);

        FileHandle fileHandle = validateHandle(handle, h, FileHandle.class);
        SftpFileSystemAccessor accessor = getFileSystemAccessor();
        ServerSession session = getServerSession();
        accessor.syncFileData(session, this, fileHandle.getFile(), fileHandle.getFileHandle(), fileHandle.getFileChannel());
    }

    protected void doCheckFileHash(Buffer buffer, int id, String targetType) throws IOException {
        String target = buffer.getString();
        String algList = buffer.getString();
        String[] algos = GenericUtils.split(algList, ',');
        long startOffset = buffer.getLong();
        long length = buffer.getLong();
        int blockSize = buffer.getInt();
        try {
            buffer.clear();
            buffer.putByte((byte) SftpConstants.SSH_FXP_EXTENDED_REPLY);
            buffer.putInt(id);
            buffer.putString(SftpConstants.EXT_CHECK_FILE);
            doCheckFileHash(id, targetType, target, Arrays.asList(algos), startOffset, length, blockSize, buffer);
        } catch (Exception e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        send(buffer);
    }

    protected void doCheckFileHash(int id, String targetType, String target, Collection<String> algos,
                                   long startOffset, long length, int blockSize, Buffer buffer)
            throws Exception {
        Path path;
        if (SftpConstants.EXT_CHECK_FILE_HANDLE.equalsIgnoreCase(targetType)) {
            Handle h = handles.get(target);
            FileHandle fileHandle = validateHandle(target, h, FileHandle.class);
            path = fileHandle.getFile();

            /*
             * To quote http://tools.ietf.org/wg/secsh/draft-ietf-secsh-filexfer/draft-ietf-secsh-filexfer-09.txt section 9.1.2:
             *
             *       If ACE4_READ_DATA was not included when the file was opened,
             *       the server MUST return STATUS_PERMISSION_DENIED.
             */
            int access = fileHandle.getAccessMask();
            if ((access & SftpConstants.ACE4_READ_DATA) == 0) {
                throw new AccessDeniedException("File not opened for read: " + path);
            }
        } else {
            path = resolveFile(target);

            /*
             * To quote http://tools.ietf.org/wg/secsh/draft-ietf-secsh-filexfer/draft-ietf-secsh-filexfer-09.txt section 9.1.2:
             *
             *      If 'check-file-name' refers to a SSH_FILEXFER_TYPE_SYMLINK, the
             *      target should be opened.
             */
            for (int index = 0; Files.isSymbolicLink(path) && (index < Byte.MAX_VALUE /* TODO make this configurable */); index++) {
                path = Files.readSymbolicLink(path);
            }

            if (Files.isSymbolicLink(path)) {
                throw new FileSystemLoopException(target + " yields a circular or too long chain of symlinks");
            }

            if (Files.isDirectory(path, IoUtils.getLinkOptions(false))) {
                throw new NotDirectoryException(path.toString());
            }
        }

        ValidateUtils.checkNotNullAndNotEmpty(algos, "No hash algorithms specified");

        DigestFactory factory = null;
        for (String a : algos) {
            factory = BuiltinDigests.fromFactoryName(a);
            if ((factory != null) && factory.isSupported()) {
                break;
            }
        }
        ValidateUtils.checkNotNull(factory, "No matching digest factory found for %s", algos);

        doCheckFileHash(id, path, factory, startOffset, length, blockSize, buffer);
    }

    protected void doCheckFileHash(int id, Path file, NamedFactory<? extends Digest> factory,
                                   long startOffset, long length, int blockSize, Buffer buffer)
            throws Exception {
        ValidateUtils.checkTrue(startOffset >= 0L, "Invalid start offset: %d", startOffset);
        ValidateUtils.checkTrue(length >= 0L, "Invalid length: %d", length);
        ValidateUtils.checkTrue((blockSize == 0) || (blockSize >= SftpConstants.MIN_CHKFILE_BLOCKSIZE), "Invalid block size: %d", blockSize);
        Objects.requireNonNull(factory, "No digest factory provided");
        buffer.putString(factory.getName());

        long effectiveLength = length;
        long totalLength = Files.size(file);
        if (effectiveLength == 0L) {
            effectiveLength = totalLength - startOffset;
        } else {
            long maxRead = startOffset + length;
            if (maxRead > totalLength) {
                effectiveLength = totalLength - startOffset;
            }
        }
        ValidateUtils.checkTrue(effectiveLength > 0L, "Non-positive effective hash data length: %d", effectiveLength);

        byte[] digestBuf = (blockSize == 0)
                ? new byte[Math.min((int) effectiveLength, IoUtils.DEFAULT_COPY_SIZE)]
                : new byte[Math.min((int) effectiveLength, blockSize)];
        ByteBuffer wb = ByteBuffer.wrap(digestBuf);
        SftpFileSystemAccessor accessor = getFileSystemAccessor();
        try (SeekableByteChannel channel = accessor.openFile(getServerSession(), this, file, "", Collections.emptySet())) {
            channel.position(startOffset);

            Digest digest = factory.create();
            digest.init();

            if (blockSize == 0) {
                while (effectiveLength > 0L) {
                    int remainLen = Math.min(digestBuf.length, (int) effectiveLength);
                    ByteBuffer bb = wb;
                    if (remainLen < digestBuf.length) {
                        bb = ByteBuffer.wrap(digestBuf, 0, remainLen);
                    }
                    bb.clear(); // prepare for next read

                    int readLen = channel.read(bb);
                    if (readLen < 0) {
                        break;
                    }

                    effectiveLength -= readLen;
                    digest.update(digestBuf, 0, readLen);
                }

                byte[] hashValue = digest.digest();
                buffer.putBytes(hashValue);
            } else {
                for (int count = 0; effectiveLength > 0L; count++) {
                    int remainLen = Math.min(digestBuf.length, (int) effectiveLength);
                    ByteBuffer bb = wb;
                    if (remainLen < digestBuf.length) {
                        bb = ByteBuffer.wrap(digestBuf, 0, remainLen);
                    }
                    bb.clear(); // prepare for next read

                    int readLen = channel.read(bb);
                    if (readLen < 0) {
                        break;
                    }

                    effectiveLength -= readLen;
                    digest.update(digestBuf, 0, readLen);

                    byte[] hashValue = digest.digest(); // NOTE: this also resets the hash for the next read
                    buffer.putBytes(hashValue);
                }
            }
        }
    }

    protected void doMD5Hash(Buffer buffer, int id, String targetType) throws IOException {
        String target = buffer.getString();
        long startOffset = buffer.getLong();
        long length = buffer.getLong();
        byte[] quickCheckHash = buffer.getBytes();
        byte[] hashValue;

        try {
            hashValue = doMD5Hash(id, targetType, target, startOffset, length, quickCheckHash);

        } catch (Exception e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        buffer.clear();
        buffer.putByte((byte) SftpConstants.SSH_FXP_EXTENDED_REPLY);
        buffer.putInt(id);
        buffer.putString(targetType);
        buffer.putBytes(hashValue);
        send(buffer);
    }

    protected byte[] doMD5Hash(
            int id, String targetType, String target, long startOffset, long length, byte[] quickCheckHash)
            throws Exception {
        Path path;
        if (SftpConstants.EXT_MD5_HASH_HANDLE.equalsIgnoreCase(targetType)) {
            Handle h = handles.get(target);
            FileHandle fileHandle = validateHandle(target, h, FileHandle.class);
            path = fileHandle.getFile();

            /*
             * To quote http://tools.ietf.org/wg/secsh/draft-ietf-secsh-filexfer/draft-ietf-secsh-filexfer-09.txt section 9.1.1:
             *
             *      The handle MUST be a file handle, and ACE4_READ_DATA MUST
             *      have been included in the desired-access when the file
             *      was opened
             */
            int access = fileHandle.getAccessMask();
            if ((access & SftpConstants.ACE4_READ_DATA) == 0) {
                throw new AccessDeniedException("File not opened for read: " + path);
            }
        } else {
            path = resolveFile(target);
            if (Files.isDirectory(path, IoUtils.getLinkOptions(true))) {
                throw new NotDirectoryException(path.toString());
            }
        }

        /*
         * To quote http://tools.ietf.org/wg/secsh/draft-ietf-secsh-filexfer/draft-ietf-secsh-filexfer-09.txt section 9.1.1:
         *
         *      If both start-offset and length are zero, the entire file should be included
         */
        long effectiveLength = length;
        long totalSize = Files.size(path);
        if ((startOffset == 0L) && (length == 0L)) {
            effectiveLength = totalSize;
        } else {
            long maxRead = startOffset + effectiveLength;
            if (maxRead > totalSize) {
                effectiveLength = totalSize - startOffset;
            }
        }

        return doMD5Hash(id, path, startOffset, effectiveLength, quickCheckHash);
    }

    protected byte[] doMD5Hash(int id, Path path, long startOffset, long length, byte[] quickCheckHash) throws Exception {
        ValidateUtils.checkTrue(startOffset >= 0L, "Invalid start offset: %d", startOffset);
        ValidateUtils.checkTrue(length > 0L, "Invalid length: %d", length);
        if (!BuiltinDigests.md5.isSupported()) {
            throw new UnsupportedOperationException(BuiltinDigests.md5.getAlgorithm() + " hash not supported");
        }

        Digest digest = BuiltinDigests.md5.create();
        digest.init();

        long effectiveLength = length;
        byte[] digestBuf = new byte[(int) Math.min(effectiveLength, SftpConstants.MD5_QUICK_HASH_SIZE)];
        ByteBuffer wb = ByteBuffer.wrap(digestBuf);
        boolean hashMatches = false;
        byte[] hashValue = null;
        SftpFileSystemAccessor accessor = getFileSystemAccessor();
        try (SeekableByteChannel channel = accessor.openFile(getServerSession(), this, path, null, EnumSet.of(StandardOpenOption.READ))) {
            channel.position(startOffset);

            /*
             * To quote http://tools.ietf.org/wg/secsh/draft-ietf-secsh-filexfer/draft-ietf-secsh-filexfer-09.txt section 9.1.1:
             *
             *      If this is a zero length string, the client does not have the
             *      data, and is requesting the hash for reasons other than comparing
             *      with a local file.  The server MAY return SSH_FX_OP_UNSUPPORTED in
             *      this case.
             */
            if (NumberUtils.length(quickCheckHash) <= 0) {
                // TODO consider limiting it - e.g., if the requested effective length is <= than some (configurable) threshold
                hashMatches = true;
            } else {
                int readLen = channel.read(wb);
                if (readLen < 0) {
                    throw new EOFException("EOF while read initial buffer from " + path);
                }
                effectiveLength -= readLen;
                digest.update(digestBuf, 0, readLen);

                hashValue = digest.digest();
                hashMatches = Arrays.equals(quickCheckHash, hashValue);
                if (hashMatches) {
                    /*
                     * Need to re-initialize the digester due to the Javadoc:
                     *
                     *      "The digest method can be called once for a given number
                     *       of updates. After digest has been called, the MessageDigest
                     *       object is reset to its initialized state."
                     */
                    if (effectiveLength > 0L) {
                        digest = BuiltinDigests.md5.create();
                        digest.init();
                        digest.update(digestBuf, 0, readLen);
                        hashValue = null;   // start again
                    }
                } else {
                }
            }

            if (hashMatches) {
                while (effectiveLength > 0L) {
                    int remainLen = Math.min(digestBuf.length, (int) effectiveLength);
                    ByteBuffer bb = wb;
                    if (remainLen < digestBuf.length) {
                        bb = ByteBuffer.wrap(digestBuf, 0, remainLen);
                    }
                    bb.clear(); // prepare for next read

                    int readLen = channel.read(bb);
                    if (readLen < 0) {
                        break;  // user may have specified more than we have available
                    }
                    effectiveLength -= readLen;
                    digest.update(digestBuf, 0, readLen);
                }

                if (hashValue == null) {    // check if did any more iterations after the quick hash
                    hashValue = digest.digest();
                }
            } else {
                hashValue = GenericUtils.EMPTY_BYTE_ARRAY;
            }
        }

        return hashValue;
    }

    protected void doVersionSelect(Buffer buffer, int id) throws IOException {
        String proposed = buffer.getString();
        ServerSession session = getServerSession();
        /*
         * The 'version-select' MUST be the first request from the client to the
         * server; if it is not, the server MUST fail the request and close the
         * channel.
         */
        if (requestsCount > 0L) {
            sendStatus(BufferUtils.clear(buffer), id,
                    SftpConstants.SSH_FX_FAILURE,
                    "Version selection not the 1st request for proposal = " + proposed);
            session.close(true);
            return;
        }

        Boolean result = validateProposedVersion(buffer, id, proposed);
        /*
         * "MUST then close the channel without processing any further requests"
         */
        if (result == null) {   // response sent internally
            session.close(true);
            return;
        }
        if (result) {
            version = Integer.parseInt(proposed);
            sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_OK, "");
        } else {
            sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_FAILURE, "Unsupported version " + proposed);
            session.close(true);
        }
    }

    /**
     * @param buffer   The {@link Buffer} holding the request
     * @param id       The request id
     * @param proposed The proposed value
     * @return A {@link Boolean} indicating whether to accept/reject the proposal.
     * If {@code null} then rejection response has been sent, otherwise and
     * appropriate response is generated
     * @throws IOException If failed send an independent rejection response
     */
    protected Boolean validateProposedVersion(Buffer buffer, int id, String proposed) throws IOException {

        if (GenericUtils.length(proposed) != 1) {
            return Boolean.FALSE;
        }

        char digit = proposed.charAt(0);
        if ((digit < '0') || (digit > '9')) {
            return Boolean.FALSE;
        }

        int value = digit - '0';
        String all = checkVersionCompatibility(buffer, id, value, SftpConstants.SSH_FX_FAILURE);
        if (GenericUtils.isEmpty(all)) {    // validation failed
            return null;
        } else {
            return Boolean.TRUE;
        }
    }

    /**
     * Checks if a proposed version is within supported range. <B>Note:</B>
     * if the user forced a specific value via the {@link #SFTP_VERSION}
     * property, then it is used to validate the proposed value
     *
     * @param buffer        The {@link Buffer} containing the request
     * @param id            The SSH message ID to be used to send the failure message
     *                      if required
     * @param proposed      The proposed version value
     * @param failureOpcode The failure opcode to send if validation fails
     * @return A {@link String} of comma separated values representing all
     * the supported version - {@code null} if validation failed and an
     * appropriate status message was sent
     * @throws IOException If failed to send the failure status message
     */
    protected String checkVersionCompatibility(Buffer buffer, int id, int proposed, int failureOpcode) throws IOException {
        int low = LOWER_SFTP_IMPL;
        int hig = HIGHER_SFTP_IMPL;
        String available = ALL_SFTP_IMPL;
        // check if user wants to use a specific version
        ServerSession session = getServerSession();
        Integer sftpVersion = session.getInteger(SFTP_VERSION);
        if (sftpVersion != null) {
            int forcedValue = sftpVersion;
            if ((forcedValue < LOWER_SFTP_IMPL) || (forcedValue > HIGHER_SFTP_IMPL)) {
                throw new IllegalStateException("Forced SFTP version (" + sftpVersion + ") not within supported values: " + available);
            }
            hig = sftpVersion;
            low = hig;
            available = sftpVersion.toString();
        }

        if ((proposed < low) || (proposed > hig)) {
            sendStatus(BufferUtils.clear(buffer), id, failureOpcode, "Proposed version (" + proposed + ") not in supported range: " + available);
            return null;
        }

        return available;
    }

    protected void doBlock(Buffer buffer, int id) throws IOException {
        String handle = buffer.getString();
        long offset = buffer.getLong();
        long length = buffer.getLong();
        int mask = buffer.getInt();

        try {
            doBlock(id, handle, offset, length, mask);
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_OK, "");
    }

    protected void doBlock(int id, String handle, long offset, long length, int mask) throws IOException {
        Handle p = handles.get(handle);

        FileHandle fileHandle = validateHandle(handle, p, FileHandle.class);
        SftpEventListener listener = getSftpEventListenerProxy();
        ServerSession session = getServerSession();
        listener.blocking(session, handle, fileHandle, offset, length, mask);
        try {
            fileHandle.lock(offset, length, mask);
        } catch (IOException | RuntimeException e) {
            listener.blocked(session, handle, fileHandle, offset, length, mask, e);
            throw e;
        }
        listener.blocked(session, handle, fileHandle, offset, length, mask, null);
    }

    protected void doUnblock(Buffer buffer, int id) throws IOException {
        String handle = buffer.getString();
        long offset = buffer.getLong();
        long length = buffer.getLong();
        try {
            doUnblock(id, handle, offset, length);
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_OK, "");
    }

    protected void doUnblock(int id, String handle, long offset, long length) throws IOException {
        Handle p = handles.get(handle);

        FileHandle fileHandle = validateHandle(handle, p, FileHandle.class);
        SftpEventListener listener = getSftpEventListenerProxy();
        ServerSession session = getServerSession();
        listener.unblocking(session, handle, fileHandle, offset, length);
        try {
            fileHandle.unlock(offset, length);
        } catch (IOException | RuntimeException e) {
            listener.unblocked(session, handle, fileHandle, offset, length, e);
            throw e;
        }
        listener.unblocked(session, handle, fileHandle, offset, length, null);
    }

    protected void doLink(Buffer buffer, int id) throws IOException {
        String targetPath = buffer.getString();
        String linkPath = buffer.getString();
        boolean symLink = buffer.getBoolean();

        try {
            doLink(id, targetPath, linkPath, symLink);
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_OK, "");
    }

    protected void doLink(int id, String targetPath, String linkPath, boolean symLink) throws IOException {
        createLink(id, targetPath, linkPath, symLink);
    }

    protected void doSymLink(Buffer buffer, int id) throws IOException {
        String targetPath = buffer.getString();
        String linkPath = buffer.getString();
        try {
            doSymLink(id, targetPath, linkPath);
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_OK, "");
    }

    protected void doSymLink(int id, String targetPath, String linkPath) throws IOException {
        createLink(id, targetPath, linkPath, true);
    }

    protected void createLink(int id, String existingPath, String linkPath, boolean symLink) throws IOException {
        Path link = resolveFile(linkPath);
        Path existing = fileSystem.getPath(existingPath);

        SftpEventListener listener = getSftpEventListenerProxy();
        ServerSession session = getServerSession();
        listener.linking(session, link, existing, symLink);
        try {
            if (symLink) {
                Files.createSymbolicLink(link, existing);
            } else {
                Files.createLink(link, existing);
            }
        } catch (IOException | RuntimeException e) {
            listener.linked(session, link, existing, symLink, e);
            throw e;
        }
        listener.linked(session, link, existing, symLink, null);
    }

    protected void doReadLink(Buffer buffer, int id) throws IOException {
        String path = buffer.getString();
        String l;
        try {
            l = doReadLink(id, path);
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendLink(BufferUtils.clear(buffer), id, l);
    }

    protected String doReadLink(int id, String path) throws IOException {
        Path f = resolveFile(path);
        Path t = Files.readSymbolicLink(f);
        return t.toString();
    }

    protected void doRename(Buffer buffer, int id) throws IOException {
        String oldPath = buffer.getString();
        String newPath = buffer.getString();
        int flags = 0;
        if (version >= SftpConstants.SFTP_V5) {
            flags = buffer.getInt();
        }
        try {
            doRename(id, oldPath, newPath, flags);
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_OK, "");
    }

    protected void doRename(int id, String oldPath, String newPath, int flags) throws IOException {

        Collection<CopyOption> opts = Collections.emptyList();
        if (flags != 0) {
            opts = new ArrayList<>();
            if ((flags & SftpConstants.SSH_FXP_RENAME_ATOMIC) == SftpConstants.SSH_FXP_RENAME_ATOMIC) {
                opts.add(StandardCopyOption.ATOMIC_MOVE);
            }
            if ((flags & SftpConstants.SSH_FXP_RENAME_OVERWRITE) == SftpConstants.SSH_FXP_RENAME_OVERWRITE) {
                opts.add(StandardCopyOption.REPLACE_EXISTING);
            }
        }

        doRename(id, oldPath, newPath, opts);
    }

    protected void doRename(int id, String oldPath, String newPath, Collection<CopyOption> opts) throws IOException {
        Path o = resolveFile(oldPath);
        Path n = resolveFile(newPath);
        SftpEventListener listener = getSftpEventListenerProxy();
        ServerSession session = getServerSession();

        listener.moving(session, o, n, opts);
        try {
            Files.move(o, n, GenericUtils.isEmpty(opts) ? IoUtils.EMPTY_COPY_OPTIONS : opts.toArray(new CopyOption[opts.size()]));
        } catch (IOException | RuntimeException e) {
            listener.moved(session, o, n, opts, e);
            throw e;
        }
        listener.moved(session, o, n, opts, null);
    }

    // see https://tools.ietf.org/html/draft-ietf-secsh-filexfer-extensions-00#section-7
    protected void doCopyData(Buffer buffer, int id) throws IOException {
        String readHandle = buffer.getString();
        long readOffset = buffer.getLong();
        long readLength = buffer.getLong();
        String writeHandle = buffer.getString();
        long writeOffset = buffer.getLong();
        try {
            doCopyData(id, readHandle, readOffset, readLength, writeHandle, writeOffset);
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_OK, "");
    }

    @SuppressWarnings("resource")
    protected void doCopyData(int id, String readHandle, long readOffset, long readLength, String writeHandle, long writeOffset) throws IOException {
        boolean inPlaceCopy = readHandle.equals(writeHandle);
        Handle rh = handles.get(readHandle);
        Handle wh = inPlaceCopy ? rh : handles.get(writeHandle);

        FileHandle srcHandle = validateHandle(readHandle, rh, FileHandle.class);
        Path srcPath = srcHandle.getFile();
        int srcAccess = srcHandle.getAccessMask();
        if ((srcAccess & SftpConstants.ACE4_READ_DATA) != SftpConstants.ACE4_READ_DATA) {
            throw new AccessDeniedException("File not opened for read: " + srcPath);
        }

        ValidateUtils.checkTrue(readLength >= 0L, "Invalid read length: %d", readLength);
        ValidateUtils.checkTrue(readOffset >= 0L, "Invalid read offset: %d", readOffset);

        long totalSize = Files.size(srcHandle.getFile());
        long effectiveLength = readLength;
        if (effectiveLength == 0L) {
            effectiveLength = totalSize - readOffset;
        } else {
            long maxRead = readOffset + effectiveLength;
            if (maxRead > totalSize) {
                effectiveLength = totalSize - readOffset;
            }
        }
        ValidateUtils.checkTrue(effectiveLength > 0L, "Non-positive effective copy data length: %d", effectiveLength);

        FileHandle dstHandle = inPlaceCopy ? srcHandle : validateHandle(writeHandle, wh, FileHandle.class);
        int dstAccess = dstHandle.getAccessMask();
        if ((dstAccess & SftpConstants.ACE4_WRITE_DATA) != SftpConstants.ACE4_WRITE_DATA) {
            throw new AccessDeniedException("File not opened for write: " + srcHandle);
        }

        ValidateUtils.checkTrue(writeOffset >= 0L, "Invalid write offset: %d", writeOffset);
        // check if overlapping ranges as per the draft
        if (inPlaceCopy) {
            long maxRead = readOffset + effectiveLength;
            if (maxRead > totalSize) {
                maxRead = totalSize;
            }

            long maxWrite = writeOffset + effectiveLength;
            if (maxWrite > readOffset) {
                throw new IllegalArgumentException("Write range end [" + writeOffset + "-" + maxWrite + "]"
                        + " overlaps with read range [" + readOffset + "-" + maxRead + "]");
            } else if (maxRead > writeOffset) {
                throw new IllegalArgumentException("Read range end [" + readOffset + "-" + maxRead + "]"
                        + " overlaps with write range [" + writeOffset + "-" + maxWrite + "]");
            }
        }

        byte[] copyBuf = new byte[Math.min(IoUtils.DEFAULT_COPY_SIZE, (int) effectiveLength)];
        while (effectiveLength > 0L) {
            int remainLength = Math.min(copyBuf.length, (int) effectiveLength);
            int readLen = srcHandle.read(copyBuf, 0, remainLength, readOffset);
            if (readLen < 0) {
                throw new EOFException("Premature EOF while still remaining " + effectiveLength + " bytes");
            }
            dstHandle.write(copyBuf, 0, readLen, writeOffset);

            effectiveLength -= readLen;
            readOffset += readLen;
            writeOffset += readLen;
        }
    }

    // see https://tools.ietf.org/html/draft-ietf-secsh-filexfer-extensions-00#section-6
    protected void doCopyFile(Buffer buffer, int id) throws IOException {
        String srcFile = buffer.getString();
        String dstFile = buffer.getString();
        boolean overwriteDestination = buffer.getBoolean();

        try {
            doCopyFile(id, srcFile, dstFile, overwriteDestination);
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_OK, "");
    }

    protected void doCopyFile(int id, String srcFile, String dstFile, boolean overwriteDestination) throws IOException {
        doCopyFile(id, srcFile, dstFile,
                overwriteDestination
                        ? Collections.singletonList(StandardCopyOption.REPLACE_EXISTING)
                        : Collections.emptyList());
    }

    protected void doCopyFile(int id, String srcFile, String dstFile, Collection<CopyOption> opts) throws IOException {
        Path src = resolveFile(srcFile);
        Path dst = resolveFile(dstFile);
        Files.copy(src, dst, GenericUtils.isEmpty(opts) ? IoUtils.EMPTY_COPY_OPTIONS : opts.toArray(new CopyOption[opts.size()]));
    }

    protected void doStat(Buffer buffer, int id) throws IOException {
        String path = buffer.getString();
        int flags = SftpConstants.SSH_FILEXFER_ATTR_ALL;
        if (version >= SftpConstants.SFTP_V4) {
            flags = buffer.getInt();
        }

        Map<String, Object> attrs;
        try {
            attrs = doStat(id, path, flags);
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendAttrs(BufferUtils.clear(buffer), id, attrs);
    }

    protected Map<String, Object> doStat(int id, String path, int flags) throws IOException {

        /*
         * SSH_FXP_STAT and SSH_FXP_LSTAT only differ in that SSH_FXP_STAT
         * follows symbolic links on the server, whereas SSH_FXP_LSTAT does not.
         */
        Path p = resolveFile(path);
        return resolveFileAttributes(p, flags, IoUtils.getLinkOptions(true));
    }

    protected void doRealPath(Buffer buffer, int id) throws IOException {
        String path = buffer.getString();
        path = GenericUtils.trimToEmpty(path);
        if (GenericUtils.isEmpty(path)) {
            path = ".";
        }

        Map<String, ?> attrs = Collections.emptyMap();
        Pair<Path, Boolean> result;
        try {
            if (version < SftpConstants.SFTP_V6) {
                /*
                 * See http://www.openssh.com/txt/draft-ietf-secsh-filexfer-02.txt:
                 *
                 *      The SSH_FXP_REALPATH request can be used to have the server
                 *      canonicalize any given path name to an absolute path.
                 *
                 * See also SSHD-294
                 */
                Path p = resolveFile(path);
                LinkOption[] options =
                        getPathResolutionLinkOption(SftpConstants.SSH_FXP_REALPATH, "", p);
                result = doRealPathV345(id, path, p, options);
            } else {
                /*
                 * See https://tools.ietf.org/html/draft-ietf-secsh-filexfer-13#section-8.9
                 *
                 *      This field is optional, and if it is not present in the packet, it
                 *      is assumed to be SSH_FXP_REALPATH_NO_CHECK.
                 */
                int control = SftpConstants.SSH_FXP_REALPATH_NO_CHECK;
                if (buffer.available() > 0) {
                    control = buffer.getUByte();
                }

                Collection<String> extraPaths = new LinkedList<>();
                while (buffer.available() > 0) {
                    extraPaths.add(buffer.getString());
                }

                Path p = resolveFile(path);
                LinkOption[] options =
                        getPathResolutionLinkOption(SftpConstants.SSH_FXP_REALPATH, "", p);
                result = doRealPathV6(id, path, extraPaths, p, options);

                p = result.getFirst();
                options = getPathResolutionLinkOption(SftpConstants.SSH_FXP_REALPATH, "", p);
                Boolean status = result.getSecond();
                switch (control) {
                    case SftpConstants.SSH_FXP_REALPATH_STAT_IF:
                        if (status == null) {
                            attrs = handleUnknownStatusFileAttributes(p, SftpConstants.SSH_FILEXFER_ATTR_ALL, options);
                        } else if (status) {
                            try {
                                attrs = getAttributes(p, options);
                            } catch (IOException e) {
                            }
                        } else {
                        }
                        break;
                    case SftpConstants.SSH_FXP_REALPATH_STAT_ALWAYS:
                        if (status == null) {
                            attrs = handleUnknownStatusFileAttributes(p, SftpConstants.SSH_FILEXFER_ATTR_ALL, options);
                        } else if (status) {
                            attrs = getAttributes(p, options);
                        } else {
                            throw new FileNotFoundException(p.toString());
                        }
                        break;
                    case SftpConstants.SSH_FXP_REALPATH_NO_CHECK:
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendPath(BufferUtils.clear(buffer), id, result.getFirst(), attrs);
    }

    protected Pair<Path, Boolean> doRealPathV6(
            int id, String path, Collection<String> extraPaths, Path p, LinkOption... options) throws IOException {
        int numExtra = GenericUtils.size(extraPaths);
        if (numExtra > 0) {
            StringBuilder sb = new StringBuilder(GenericUtils.length(path) + numExtra * 8);
            sb.append(path);

            for (String p2 : extraPaths) {
                p = p.resolve(p2);
                options = getPathResolutionLinkOption(SftpConstants.SSH_FXP_REALPATH, "", p);
                sb.append('/').append(p2);
            }

            path = sb.toString();
        }

        return validateRealPath(id, path, p, options);
    }

    protected Pair<Path, Boolean> doRealPathV345(int id, String path, Path p, LinkOption... options) throws IOException {
        return validateRealPath(id, path, p, options);
    }

    /**
     * @param id      The request identifier
     * @param path    The original path
     * @param f       The resolve {@link Path}
     * @param options The {@link LinkOption}s to use to verify file existence and access
     * @return A {@link Pair} whose left-hand is the <U>absolute <B>normalized</B></U>
     * {@link Path} and right-hand is a {@link Boolean} indicating its status
     * @throws IOException If failed to validate the file
     * @see IoUtils#checkFileExists(Path, LinkOption...)
     */
    protected Pair<Path, Boolean> validateRealPath(int id, String path, Path f, LinkOption... options) throws IOException {
        Path p = normalize(f);
        Boolean status = IoUtils.checkFileExists(p, options);
        return new Pair<>(p, status);
    }

    protected void doRemoveDirectory(Buffer buffer, int id) throws IOException {
        String path = buffer.getString();
        try {
            doRemoveDirectory(id, path, IoUtils.getLinkOptions(false));
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_OK, "");
    }

    protected void doRemoveDirectory(int id, String path, LinkOption... options) throws IOException {
        Path p = resolveFile(path);
        if (Files.isDirectory(p, options)) {
            doRemove(id, p);
        } else {
            throw new NotDirectoryException(p.toString());
        }
    }

    /**
     * Called when need to delete a file / directory - also informs the {@link SftpEventListener}
     *
     * @param id Deletion request ID
     * @param p  {@link Path} to delete
     * @throws IOException If failed to delete
     */
    protected void doRemove(int id, Path p) throws IOException {
        SftpEventListener listener = getSftpEventListenerProxy();
        ServerSession session = getServerSession();
        listener.removing(session, p);
        try {
            Files.delete(p);
        } catch (IOException | RuntimeException e) {
            listener.removed(session, p, e);
            throw e;
        }
        listener.removed(session, p, null);
    }

    protected void doMakeDirectory(Buffer buffer, int id) throws IOException {
        String path = buffer.getString();
        Map<String, Object> attrs = readAttrs(buffer);
        try {
            doMakeDirectory(id, path, attrs, IoUtils.getLinkOptions(false));
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_OK, "");
    }

    protected void doMakeDirectory(int id, String path, Map<String, ?> attrs, LinkOption... options) throws IOException {
        Path p = resolveFile(path);
        Boolean status = IoUtils.checkFileExists(p, options);
        if (status == null) {
            throw new AccessDeniedException("Cannot validate make-directory existence for " + p);
        }

        if (status) {
            if (Files.isDirectory(p, options)) {
                throw new FileAlreadyExistsException(p.toString(), p.toString(), "Target directory already exists");
            } else {
                throw new FileNotFoundException(p.toString() + " already exists as a file");
            }
        } else {
            SftpEventListener listener = getSftpEventListenerProxy();
            ServerSession session = getServerSession();
            listener.creating(session, p, attrs);
            try {
                Files.createDirectory(p);
                doSetAttributes(p, attrs);
            } catch (IOException | RuntimeException e) {
                listener.created(session, p, attrs, e);
                throw e;
            }
            listener.created(session, p, attrs, null);
        }
    }

    protected void doRemove(Buffer buffer, int id) throws IOException {
        String path = buffer.getString();
        try {
            /*
             * If 'filename' is a symbolic link, the link is removed,
             * not the file it points to.
             */
            doRemove(id, path, IoUtils.getLinkOptions(false));
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_OK, "");
    }

    protected void doRemove(int id, String path, LinkOption... options) throws IOException {
        Path p = resolveFile(path);
        Boolean status = IoUtils.checkFileExists(p, options);
        if (status == null) {
            throw new AccessDeniedException("Cannot determine existence of remove candidate: " + p);
        }
        if (!status) {
            throw new FileNotFoundException(p.toString());
        } else if (Files.isDirectory(p, options)) {
            throw new SftpException(SftpConstants.SSH_FX_FILE_IS_A_DIRECTORY, p.toString() + " is a folder");
        } else {
            doRemove(id, p);
        }
    }

    protected void doReadDir(Buffer buffer, int id) throws IOException {
        String handle = buffer.getString();
        Handle h = handles.get(handle);
        Buffer reply = null;
        try {
            DirectoryHandle dh = validateHandle(handle, h, DirectoryHandle.class);
            if (dh.isDone()) {
                sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_EOF, "Directory reading is done");
                return;
            }

            Path file = dh.getFile();
            LinkOption[] options =
                    getPathResolutionLinkOption(SftpConstants.SSH_FXP_READDIR, "", file);
            Boolean status = IoUtils.checkFileExists(file, options);
            if (status == null) {
                throw new AccessDeniedException("Cannot determine existence of read-dir for " + file);
            }

            if (!status) {
                throw new FileNotFoundException(file.toString());
            } else if (!Files.isDirectory(file, options)) {
                throw new NotDirectoryException(file.toString());
            } else if (!Files.isReadable(file)) {
                throw new AccessDeniedException("Not readable: " + file.toString());
            }

            if (dh.isSendDot() || dh.isSendDotDot() || dh.hasNext()) {
                // There is at least one file in the directory or we need to send the "..".
                // Send only a few files at a time to not create packets of a too
                // large size or have a timeout to occur.

                reply = BufferUtils.clear(buffer);
                reply.putByte((byte) SftpConstants.SSH_FXP_NAME);
                reply.putInt(id);

                int lenPos = reply.wpos();
                reply.putInt(0);

                ServerSession session = getServerSession();
                int maxDataSize = session.getIntProperty(MAX_READDIR_DATA_SIZE_PROP, DEFAULT_MAX_READDIR_DATA_SIZE);
                int count = doReadDir(id, handle, dh, reply, maxDataSize, IoUtils.getLinkOptions(false));
                BufferUtils.updateLengthPlaceholder(reply, lenPos, count);
                if ((!dh.isSendDot()) && (!dh.isSendDotDot()) && (!dh.hasNext())) {
                    dh.markDone();
                }

                Boolean indicator =
                        SftpHelper.indicateEndOfNamesList(reply, getVersion(), session, dh.isDone());
            } else {
                // empty directory
                dh.markDone();
                sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_EOF, "Empty directory");
                return;
            }

            Objects.requireNonNull(reply, "No reply buffer created");
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        send(reply);
    }

    protected void doOpenDir(Buffer buffer, int id) throws IOException {
        String path = buffer.getString();
        String handle;

        try {
            Path p = resolveNormalizedLocation(path);
            LinkOption[] options =
                    getPathResolutionLinkOption(SftpConstants.SSH_FXP_OPENDIR, "", p);
            handle = doOpenDir(id, path, p, options);
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendHandle(BufferUtils.clear(buffer), id, handle);
    }

    protected String doOpenDir(int id, String path, Path p, LinkOption... options) throws IOException {
        Boolean status = IoUtils.checkFileExists(p, options);
        if (status == null) {
            throw new AccessDeniedException("Cannot determine open-dir existence for " + p);
        }

        if (!status) {
            throw new FileNotFoundException(path);
        } else if (!Files.isDirectory(p, options)) {
            throw new NotDirectoryException(path);
        } else if (!Files.isReadable(p)) {
            throw new AccessDeniedException("Not readable: " + p);
        } else {
            String handle = generateFileHandle(p);
            DirectoryHandle dirHandle = new DirectoryHandle(this, p, handle);
            handles.put(handle, dirHandle);
            return handle;
        }
    }

    protected void doFSetStat(Buffer buffer, int id) throws IOException {
        String handle = buffer.getString();
        Map<String, Object> attrs = readAttrs(buffer);
        try {
            doFSetStat(id, handle, attrs);
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_OK, "");
    }

    protected void doFSetStat(int id, String handle, Map<String, ?> attrs) throws IOException {
        Handle h = handles.get(handle);
        doSetAttributes(validateHandle(handle, h, Handle.class).getFile(), attrs);
    }

    protected void doSetStat(Buffer buffer, int id) throws IOException {
        String path = buffer.getString();
        Map<String, Object> attrs = readAttrs(buffer);
        try {
            doSetStat(id, path, attrs);
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_OK, "");
    }

    protected void doSetStat(int id, String path, Map<String, ?> attrs) throws IOException {
        Path p = resolveFile(path);
        doSetAttributes(p, attrs);
    }

    protected void doFStat(Buffer buffer, int id) throws IOException {
        String handle = buffer.getString();
        int flags = SftpConstants.SSH_FILEXFER_ATTR_ALL;
        if (version >= SftpConstants.SFTP_V4) {
            flags = buffer.getInt();
        }

        Map<String, ?> attrs;
        try {
            attrs = doFStat(id, handle, flags);
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendAttrs(BufferUtils.clear(buffer), id, attrs);
    }

    protected Map<String, Object> doFStat(int id, String handle, int flags) throws IOException {
        Handle h = handles.get(handle);
        return resolveFileAttributes(validateHandle(handle, h, Handle.class).getFile(), flags, IoUtils.getLinkOptions(true));
    }

    protected void doLStat(Buffer buffer, int id) throws IOException {
        String path = buffer.getString();
        int flags = SftpConstants.SSH_FILEXFER_ATTR_ALL;
        if (version >= SftpConstants.SFTP_V4) {
            flags = buffer.getInt();
        }

        Map<String, ?> attrs;
        try {
            attrs = doLStat(id, path, flags);
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendAttrs(BufferUtils.clear(buffer), id, attrs);
    }

    protected Map<String, Object> doLStat(int id, String path, int flags) throws IOException {
        Path p = resolveFile(path);
        /*
         * SSH_FXP_STAT and SSH_FXP_LSTAT only differ in that SSH_FXP_STAT
         * follows symbolic links on the server, whereas SSH_FXP_LSTAT does not.
         */
        return resolveFileAttributes(p, flags, IoUtils.getLinkOptions(false));
    }

    protected void doWrite(Buffer buffer, int id) throws IOException {
        String handle = buffer.getString();
        long offset = buffer.getLong();
        int length = buffer.getInt();
        try {
            doWrite(id, handle, offset, length, buffer.array(), buffer.rpos(), buffer.available());
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_OK, "");
    }

    protected void doWrite(int id, String handle, long offset, int length, byte[] data, int doff, int remaining) throws IOException {
        Handle h = handles.get(handle);
        FileHandle fh = validateHandle(handle, h, FileHandle.class);
        if (length < 0) {
            throw new IllegalStateException("Bad length (" + length + ") for writing to " + fh);
        }

        if (remaining < length) {
            throw new IllegalStateException("Not enough buffer data for writing to " + fh + ": required=" + length + ", available=" + remaining);
        }

        SftpEventListener listener = getSftpEventListenerProxy();
        listener.writing(getServerSession(), handle, fh, offset, data, doff, length);
        try {
            if (fh.isOpenAppend()) {
                fh.append(data, doff, length);
            } else {
                fh.write(data, doff, length, offset);
            }
        } catch (IOException | RuntimeException e) {
            listener.written(getServerSession(), handle, fh, offset, data, doff, length, e);
            throw e;
        }
        listener.written(getServerSession(), handle, fh, offset, data, doff, length, null);
    }

    protected void doRead(Buffer buffer, int id) throws IOException {
        String handle = buffer.getString();
        long offset = buffer.getLong();
        int requestedLength = buffer.getInt();
        int maxAllowed = getServerSession().getIntProperty(MAX_READDATA_PACKET_LENGTH_PROP, DEFAULT_MAX_READDATA_PACKET_LENGTH);
        int readLen = Math.min(requestedLength, maxAllowed);
        try {
            ValidateUtils.checkTrue(readLen >= 0, "Illegal requested read length: %d", readLen);

            buffer.clear();
            buffer.ensureCapacity(readLen + Long.SIZE /* the header */, IntUnaryOperator.identity());

            buffer.putByte((byte) SftpConstants.SSH_FXP_DATA);
            buffer.putInt(id);
            int lenPos = buffer.wpos();
            buffer.putInt(0);

            int startPos = buffer.wpos();
            int len = doRead(id, handle, offset, readLen, buffer.array(), startPos);
            if (len < 0) {
                throw new EOFException("Unable to read " + readLen + " bytes from offset=" + offset + " of " + handle);
            }
            buffer.wpos(startPos + len);
            BufferUtils.updateLengthPlaceholder(buffer, lenPos, len);
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        send(buffer);
    }

    protected int doRead(int id, String handle, long offset, int length, byte[] data, int doff) throws IOException {
        Handle h = handles.get(handle);
        ValidateUtils.checkTrue(length > 0L, "Invalid read length: %d", length);
        FileHandle fh = validateHandle(handle, h, FileHandle.class);
        SftpEventListener listener = getSftpEventListenerProxy();
        ServerSession serverSession = getServerSession();
        int readLen;
        listener.reading(serverSession, handle, fh, offset, data, doff, length);
        try {
            readLen = fh.read(data, doff, length, offset);
        } catch (IOException | RuntimeException e) {
            listener.read(serverSession, handle, fh, offset, data, doff, length, -1, e);
            throw e;
        }
        listener.read(serverSession, handle, fh, offset, data, doff, length, readLen, null);
        return readLen;
    }

    protected void doClose(Buffer buffer, int id) throws IOException {
        String handle = buffer.getString();
        try {
            doClose(id, handle);
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendStatus(BufferUtils.clear(buffer), id, SftpConstants.SSH_FX_OK, "", "");
    }

    protected void doClose(int id, String handle) throws IOException {
        Handle h = handles.remove(handle);
        validateHandle(handle, h, Handle.class).close();

        SftpEventListener listener = getSftpEventListenerProxy();
        listener.close(getServerSession(), handle, h);
    }

    protected void doOpen(Buffer buffer, int id) throws IOException {
        String path = buffer.getString();
        /*
         * Be consistent with FileChannel#open - if no mode specified then READ is assumed
         */
        int access = 0;
        if (version >= SftpConstants.SFTP_V5) {
            access = buffer.getInt();
            if (access == 0) {
                access = SftpConstants.ACE4_READ_DATA | SftpConstants.ACE4_READ_ATTRIBUTES;
            }
        }

        int pflags = buffer.getInt();
        if (pflags == 0) {
            pflags = SftpConstants.SSH_FXF_READ;
        }

        if (version < SftpConstants.SFTP_V5) {
            int flags = pflags;
            pflags = 0;
            switch (flags & (SftpConstants.SSH_FXF_READ | SftpConstants.SSH_FXF_WRITE)) {
                case SftpConstants.SSH_FXF_READ:
                    access |= SftpConstants.ACE4_READ_DATA | SftpConstants.ACE4_READ_ATTRIBUTES;
                    break;
                case SftpConstants.SSH_FXF_WRITE:
                    access |= SftpConstants.ACE4_WRITE_DATA | SftpConstants.ACE4_WRITE_ATTRIBUTES;
                    break;
                default:
                    access |= SftpConstants.ACE4_READ_DATA | SftpConstants.ACE4_READ_ATTRIBUTES;
                    access |= SftpConstants.ACE4_WRITE_DATA | SftpConstants.ACE4_WRITE_ATTRIBUTES;
                    break;
            }
            if ((flags & SftpConstants.SSH_FXF_APPEND) != 0) {
                access |= SftpConstants.ACE4_APPEND_DATA;
                pflags |= SftpConstants.SSH_FXF_APPEND_DATA | SftpConstants.SSH_FXF_APPEND_DATA_ATOMIC;
            }
            if ((flags & SftpConstants.SSH_FXF_CREAT) != 0) {
                if ((flags & SftpConstants.SSH_FXF_EXCL) != 0) {
                    pflags |= SftpConstants.SSH_FXF_CREATE_NEW;
                } else if ((flags & SftpConstants.SSH_FXF_TRUNC) != 0) {
                    pflags |= SftpConstants.SSH_FXF_CREATE_TRUNCATE;
                } else {
                    pflags |= SftpConstants.SSH_FXF_OPEN_OR_CREATE;
                }
            } else {
                if ((flags & SftpConstants.SSH_FXF_TRUNC) != 0) {
                    pflags |= SftpConstants.SSH_FXF_TRUNCATE_EXISTING;
                } else {
                    pflags |= SftpConstants.SSH_FXF_OPEN_EXISTING;
                }
            }
        }

        Map<String, Object> attrs = readAttrs(buffer);
        String handle;
        try {
            handle = doOpen(id, path, pflags, access, attrs);
        } catch (IOException | RuntimeException e) {
            sendStatus(BufferUtils.clear(buffer), id, e);
            return;
        }

        sendHandle(BufferUtils.clear(buffer), id, handle);
    }

    /**
     * @param id     Request id
     * @param path   Path
     * @param pflags Open mode flags - see {@code SSH_FXF_XXX} flags
     * @param access Access mode flags - see {@code ACE4_XXX} flags
     * @param attrs  Requested attributes
     * @return The assigned (opaque) handle
     * @throws IOException if failed to execute
     */
    protected String doOpen(int id, String path, int pflags, int access, Map<String, Object> attrs) throws IOException {
        int curHandleCount = handles.size();
        int maxHandleCount = getServerSession().getIntProperty(MAX_OPEN_HANDLES_PER_SESSION, DEFAULT_MAX_OPEN_HANDLES);
        if (curHandleCount > maxHandleCount) {
            throw new IllegalStateException("Too many open handles: current=" + curHandleCount + ", max.=" + maxHandleCount);
        }

        Path file = resolveFile(path);
        String handle = generateFileHandle(file);
        FileHandle fileHandle = new FileHandle(this, file, handle, pflags, access, attrs);
        handles.put(handle, fileHandle);
        return handle;
    }

    // we stringify our handles and treat them as such on decoding as well as it is easier to use as a map key
    protected String generateFileHandle(Path file) {
        // use several rounds in case the file handle size is relatively small so we might get conflicts
        for (int index = 0; index < maxFileHandleRounds; index++) {
            randomizer.fill(workBuf, 0, fileHandleSize);
            String handle = BufferUtils.toHex(workBuf, 0, fileHandleSize, BufferUtils.EMPTY_HEX_SEPARATOR);
            if (handles.containsKey(handle)) {
                continue;
            }
            return handle;
        }

        throw new IllegalStateException("Failed to generate a unique file handle for " + file);
    }

    protected void doInit(Buffer buffer, int id) throws IOException {
        String all = checkVersionCompatibility(buffer, id, id, SftpConstants.SSH_FX_OP_UNSUPPORTED);
        if (GenericUtils.isEmpty(all)) { // i.e. validation failed
            return;
        }

        version = id;
        while (buffer.available() > 0) {
            String name = buffer.getString();
            byte[] data = buffer.getBytes();
            extensions.put(name, data);
        }

        buffer.clear();

        buffer.putByte((byte) SftpConstants.SSH_FXP_VERSION);
        buffer.putInt(version);
        appendExtensions(buffer, all);

        SftpEventListener listener = getSftpEventListenerProxy();
        listener.initialized(getServerSession(), version);

        send(buffer);
    }

    protected void appendExtensions(Buffer buffer, String supportedVersions) {
        appendVersionsExtension(buffer, supportedVersions);
        appendNewlineExtension(buffer, resolveNewlineValue(getServerSession()));
        appendVendorIdExtension(buffer, VersionProperties.getVersionProperties());
        appendOpenSSHExtensions(buffer);
        appendAclSupportedExtension(buffer);

        Map<String, OptionalFeature> extensions = getSupportedClientExtensions();
        int numExtensions = GenericUtils.size(extensions);
        List<String> extras = (numExtensions <= 0) ? Collections.emptyList() : new ArrayList<>(numExtensions);
        if (numExtensions > 0) {
            ServerSession session = getServerSession();
            extensions.forEach((name, f) -> {
                if (!f.isSupported()) {
                    return;
                }

                extras.add(name);
            });
        }
        appendSupportedExtension(buffer, extras);
        appendSupported2Extension(buffer, extras);
    }

    protected int appendAclSupportedExtension(Buffer buffer) {
        ServerSession session = getServerSession();
        Collection<Integer> maskValues = resolveAclSupportedCapabilities(session);
        int mask = AclSupportedParser.AclCapabilities.constructAclCapabilities(maskValues);
        if (mask != 0) {
            buffer.putString(SftpConstants.EXT_ACL_SUPPORTED);

            // placeholder for length
            int lenPos = buffer.wpos();
            buffer.putInt(0);
            buffer.putInt(mask);
            BufferUtils.updateLengthPlaceholder(buffer, lenPos);
        }

        return mask;
    }

    protected Collection<Integer> resolveAclSupportedCapabilities(ServerSession session) {
        String override = session.getString(ACL_SUPPORTED_MASK_PROP);
        if (override == null) {
            return DEFAULT_ACL_SUPPORTED_MASK;
        }

        // empty means not supported

        if (override.length() == 0) {
            return Collections.emptySet();
        }

        String[] names = GenericUtils.split(override, ',');
        Set<Integer> maskValues = new HashSet<>(names.length);
        for (String n : names) {
            Integer v = ValidateUtils.checkNotNull(
                    AclSupportedParser.AclCapabilities.getAclCapabilityValue(n), "Unknown ACL capability: %s", n);
            maskValues.add(v);
        }

        return maskValues;
    }

    protected List<OpenSSHExtension> appendOpenSSHExtensions(Buffer buffer) {
        List<OpenSSHExtension> extList = resolveOpenSSHExtensions(getServerSession());
        if (GenericUtils.isEmpty(extList)) {
            return extList;
        }

        for (OpenSSHExtension ext : extList) {
            buffer.putString(ext.getName());
            buffer.putString(ext.getVersion());
        }

        return extList;
    }

    protected List<OpenSSHExtension> resolveOpenSSHExtensions(ServerSession session) {
        String value = session.getString(OPENSSH_EXTENSIONS_PROP);
        if (value == null) {    // No override
            return DEFAULT_OPEN_SSH_EXTENSIONS;
        }

        String[] pairs = GenericUtils.split(value, ',');
        int numExts = GenericUtils.length(pairs);
        if (numExts <= 0) {     // User does not want to report ANY extensions
            return Collections.emptyList();
        }

        List<OpenSSHExtension> extList = new ArrayList<>(numExts);
        for (String nvp : pairs) {
            nvp = GenericUtils.trimToEmpty(nvp);
            if (GenericUtils.isEmpty(nvp)) {
                continue;
            }

            int pos = nvp.indexOf('=');
            ValidateUtils.checkTrue((pos > 0) && (pos < (nvp.length() - 1)), "Malformed OpenSSH extension spec: %s", nvp);
            String name = GenericUtils.trimToEmpty(nvp.substring(0, pos));
            String version = GenericUtils.trimToEmpty(nvp.substring(pos + 1));
            extList.add(new OpenSSHExtension(name, ValidateUtils.checkNotNullAndNotEmpty(version, "No version specified for OpenSSH extension %s", name)));
        }

        return extList;
    }

    protected Map<String, OptionalFeature> getSupportedClientExtensions() {
        ServerSession session = getServerSession();
        String value = session.getString(CLIENT_EXTENSIONS_PROP);
        if (value == null) {
            return DEFAULT_SUPPORTED_CLIENT_EXTENSIONS;
        }
        if (value.length() <= 0) {  // means don't report any extensions
            return Collections.emptyMap();
        }

        if (value.indexOf(',') <= 0) {
            return Collections.singletonMap(value, OptionalFeature.TRUE);
        }

        String[] comps = GenericUtils.split(value, ',');
        Map<String, OptionalFeature> result = new LinkedHashMap<>(comps.length);
        for (String c : comps) {
            result.put(c, OptionalFeature.TRUE);
        }

        return result;
    }

    /**
     * Appends the &quot;versions&quot; extension to the buffer. <B>Note:</B>
     * if overriding this method make sure you either do not append anything
     * or use the correct extension name
     *
     * @param buffer The {@link Buffer} to append to
     * @param value  The recommended value - ignored if {@code null}/empty
     * @see SftpConstants#EXT_VERSIONS
     */
    protected void appendVersionsExtension(Buffer buffer, String value) {
        if (GenericUtils.isEmpty(value)) {
            return;
        }

        buffer.putString(SftpConstants.EXT_VERSIONS);
        buffer.putString(value);
    }

    /**
     * Appends the &quot;newline&quot; extension to the buffer. <B>Note:</B>
     * if overriding this method make sure you either do not append anything
     * or use the correct extension name
     *
     * @param buffer The {@link Buffer} to append to
     * @param value  The recommended value - ignored if {@code null}/empty
     * @see SftpConstants#EXT_NEWLINE
     */
    protected void appendNewlineExtension(Buffer buffer, String value) {
        if (GenericUtils.isEmpty(value)) {
            return;
        }

        buffer.putString(SftpConstants.EXT_NEWLINE);
        buffer.putString(value);
    }

    protected String resolveNewlineValue(ServerSession session) {
        String value = session.getString(NEWLINE_VALUE);
        if (value == null) {
            return IoUtils.EOL;
        } else {
            return value;   // empty means disabled
        }
    }

    /**
     * Appends the &quot;vendor-id&quot; extension to the buffer. <B>Note:</B>
     * if overriding this method make sure you either do not append anything
     * or use the correct extension name
     *
     * @param buffer            The {@link Buffer} to append to
     * @param versionProperties The currently available version properties - ignored
     *                          if {@code null}/empty. The code expects the following values:
     *                          <UL>
     *                          <LI>{@code groupId} - as the vendor name</LI>
     *                          <LI>{@code artifactId} - as the product name</LI>
     *                          <LI>{@code version} - as the product version</LI>
     *                          </UL>
     * @see SftpConstants#EXT_VENDOR_ID
     * @see <A HREF="http://tools.ietf.org/wg/secsh/draft-ietf-secsh-filexfer/draft-ietf-secsh-filexfer-09.txt">DRAFT 09 - section 4.4</A>
     */
    protected void appendVendorIdExtension(Buffer buffer, Map<String, ?> versionProperties) {
        if (GenericUtils.isEmpty(versionProperties)) {
            return;
        }

        buffer.putString(SftpConstants.EXT_VENDOR_ID);

        PropertyResolver resolver = PropertyResolverUtils.toPropertyResolver(Collections.unmodifiableMap(versionProperties));
        // placeholder for length
        int lenPos = buffer.wpos();
        buffer.putInt(0);
        buffer.putString(resolver.getStringProperty("groupId", getClass().getPackage().getName()));   // vendor-name
        buffer.putString(resolver.getStringProperty("artifactId", getClass().getSimpleName()));       // product-name
        buffer.putString(resolver.getStringProperty("version", FactoryManager.DEFAULT_VERSION));      // product-version
        buffer.putLong(0L); // product-build-number
        BufferUtils.updateLengthPlaceholder(buffer, lenPos);
    }

    /**
     * Appends the &quot;supported&quot; extension to the buffer. <B>Note:</B>
     * if overriding this method make sure you either do not append anything
     * or use the correct extension name
     *
     * @param buffer The {@link Buffer} to append to
     * @param extras The extra extensions that are available and can be reported
     *               - may be {@code null}/empty
     */
    protected void appendSupportedExtension(Buffer buffer, Collection<String> extras) {
        buffer.putString(SftpConstants.EXT_SUPPORTED);

        int lenPos = buffer.wpos();
        buffer.putInt(0); // length placeholder
        // supported-attribute-mask
        buffer.putInt(SftpConstants.SSH_FILEXFER_ATTR_SIZE | SftpConstants.SSH_FILEXFER_ATTR_PERMISSIONS
                | SftpConstants.SSH_FILEXFER_ATTR_ACCESSTIME | SftpConstants.SSH_FILEXFER_ATTR_CREATETIME
                | SftpConstants.SSH_FILEXFER_ATTR_MODIFYTIME | SftpConstants.SSH_FILEXFER_ATTR_OWNERGROUP
                | SftpConstants.SSH_FILEXFER_ATTR_BITS);
        // TODO: supported-attribute-bits
        buffer.putInt(0);
        // supported-open-flags
        buffer.putInt(SftpConstants.SSH_FXF_READ | SftpConstants.SSH_FXF_WRITE | SftpConstants.SSH_FXF_APPEND
                | SftpConstants.SSH_FXF_CREAT | SftpConstants.SSH_FXF_TRUNC | SftpConstants.SSH_FXF_EXCL);
        // TODO: supported-access-mask
        buffer.putInt(0);
        // max-read-size
        buffer.putInt(0);
        // supported extensions
        buffer.putStringList(extras, false);

        BufferUtils.updateLengthPlaceholder(buffer, lenPos);
    }

    /**
     * Appends the &quot;supported2&quot; extension to the buffer. <B>Note:</B>
     * if overriding this method make sure you either do not append anything
     * or use the correct extension name
     *
     * @param buffer The {@link Buffer} to append to
     * @param extras The extra extensions that are available and can be reported
     *               - may be {@code null}/empty
     * @see SftpConstants#EXT_SUPPORTED
     * @see <A HREF="https://tools.ietf.org/html/draft-ietf-secsh-filexfer-13#page-10">DRAFT 13 section 5.4</A>
     */
    protected void appendSupported2Extension(Buffer buffer, Collection<String> extras) {
        buffer.putString(SftpConstants.EXT_SUPPORTED2);

        int lenPos = buffer.wpos();
        buffer.putInt(0); // length placeholder
        // supported-attribute-mask
        buffer.putInt(SftpConstants.SSH_FILEXFER_ATTR_SIZE | SftpConstants.SSH_FILEXFER_ATTR_PERMISSIONS
                | SftpConstants.SSH_FILEXFER_ATTR_ACCESSTIME | SftpConstants.SSH_FILEXFER_ATTR_CREATETIME
                | SftpConstants.SSH_FILEXFER_ATTR_MODIFYTIME | SftpConstants.SSH_FILEXFER_ATTR_OWNERGROUP
                | SftpConstants.SSH_FILEXFER_ATTR_BITS);
        // TODO: supported-attribute-bits
        buffer.putInt(0);
        // supported-open-flags
        buffer.putInt(SftpConstants.SSH_FXF_ACCESS_DISPOSITION | SftpConstants.SSH_FXF_APPEND_DATA);
        // TODO: supported-access-mask
        buffer.putInt(0);
        // max-read-size
        buffer.putInt(0);
        // supported-open-block-vector
        buffer.putShort(0);
        // supported-block-vector
        buffer.putShort(0);
        // attrib-extension-count + attributes name
        buffer.putStringList(Collections.<String>emptyList(), true);
        // extension-count + supported extensions
        buffer.putStringList(extras, true);

        BufferUtils.updateLengthPlaceholder(buffer, lenPos);
    }

    protected void sendHandle(Buffer buffer, int id, String handle) throws IOException {
        buffer.putByte((byte) SftpConstants.SSH_FXP_HANDLE);
        buffer.putInt(id);
        buffer.putString(handle);
        send(buffer);
    }

    protected void sendAttrs(Buffer buffer, int id, Map<String, ?> attributes) throws IOException {
        buffer.putByte((byte) SftpConstants.SSH_FXP_ATTRS);
        buffer.putInt(id);
        writeAttrs(buffer, attributes);
        send(buffer);
    }

    protected void sendLink(Buffer buffer, int id, String link) throws IOException {
        //in case we are running on Windows
        String unixPath = link.replace(File.separatorChar, '/');
        //normalize the given path, use *nix style separator
        String normalizedPath = SelectorUtils.normalizePath(unixPath, "/");

        buffer.putByte((byte) SftpConstants.SSH_FXP_NAME);
        buffer.putInt(id);
        buffer.putInt(1);   // one response
        buffer.putString(normalizedPath);

        /*
         * As per the spec (https://tools.ietf.org/html/draft-ietf-secsh-filexfer-02#section-6.10):
         *
         *      The server will respond with a SSH_FXP_NAME packet containing only
         *      one name and a dummy attributes value.
         */
        Map<String, Object> attrs = Collections.emptyMap();
        if (version == SftpConstants.SFTP_V3) {
            buffer.putString(SftpHelper.getLongName(normalizedPath, attrs));
        }

        writeAttrs(buffer, attrs);
        SftpHelper.indicateEndOfNamesList(buffer, getVersion(), getServerSession());
        send(buffer);
    }

    protected void sendPath(Buffer buffer, int id, Path f, Map<String, ?> attrs) throws IOException {
        buffer.putByte((byte) SftpConstants.SSH_FXP_NAME);
        buffer.putInt(id);
        buffer.putInt(1);   // one reply

        String originalPath = f.toString();
        //in case we are running on Windows
        String unixPath = originalPath.replace(File.separatorChar, '/');
        //normalize the given path, use *nix style separator
        String normalizedPath = SelectorUtils.normalizePath(unixPath, "/");
        if (normalizedPath.length() == 0) {
            normalizedPath = "/";
        }
        buffer.putString(normalizedPath);

        if (version == SftpConstants.SFTP_V3) {
            f = resolveFile(normalizedPath);
            buffer.putString(getLongName(f, getShortName(f), attrs));
        }

        writeAttrs(buffer, attrs);
        SftpHelper.indicateEndOfNamesList(buffer, getVersion(), getServerSession());
        send(buffer);
    }

    /**
     * @param id      Request id
     * @param handle  The (opaque) handle assigned to this directory
     * @param dir     The {@link DirectoryHandle}
     * @param buffer  The {@link Buffer} to write the results
     * @param maxSize Max. buffer size
     * @param options The {@link LinkOption}-s to use when querying the directory contents
     * @return Number of written entries
     * @throws IOException If failed to generate an entry
     */
    protected int doReadDir(
            int id, String handle, DirectoryHandle dir, Buffer buffer, int maxSize, LinkOption... options) throws IOException {
        int nb = 0;
        Map<String, Path> entries = new TreeMap<>(Comparator.naturalOrder());
        while ((dir.isSendDot() || dir.isSendDotDot() || dir.hasNext()) && (buffer.wpos() < maxSize)) {
            if (dir.isSendDot()) {
                writeDirEntry(id, dir, entries, buffer, nb, dir.getFile(), ".", options);
                dir.markDotSent();    // do not send it again
            } else if (dir.isSendDotDot()) {
                Path dirPath = dir.getFile();
                writeDirEntry(id, dir, entries, buffer, nb, dirPath.getParent(), "..", options);
                dir.markDotDotSent(); // do not send it again
            } else {
                Path f = dir.next();
                writeDirEntry(id, dir, entries, buffer, nb, f, getShortName(f), options);
            }

            nb++;
        }

        SftpEventListener listener = getSftpEventListenerProxy();
        listener.read(getServerSession(), handle, dir, entries);
        return nb;
    }

    /**
     * @param id        Request id
     * @param dir       The {@link DirectoryHandle}
     * @param entries   An in / out {@link Map} for updating the written entry -
     *                  key = short name, value = entry {@link Path}
     * @param buffer    The {@link Buffer} to write the results
     * @param index     Zero-based index of the entry to be written
     * @param f         The entry {@link Path}
     * @param shortName The entry short name
     * @param options   The {@link LinkOption}s to use for querying the entry-s attributes
     * @throws IOException If failed to generate the entry data
     */
    protected void writeDirEntry(
            int id, DirectoryHandle dir, Map<String, Path> entries, Buffer buffer, int index, Path f, String shortName, LinkOption... options)
            throws IOException {
        Map<String, ?> attrs = resolveFileAttributes(f, SftpConstants.SSH_FILEXFER_ATTR_ALL, options);
        entries.put(shortName, f);

        buffer.putString(shortName);
        if (version == SftpConstants.SFTP_V3) {
            String longName = getLongName(f, shortName, options);
            buffer.putString(longName);
        } else {
        }

        writeAttrs(buffer, attrs);
    }

    protected String getLongName(Path f, String shortName, LinkOption... options) throws IOException {
        return getLongName(f, shortName, true, options);
    }

    protected String getLongName(Path f, String shortName, boolean sendAttrs, LinkOption... options) throws IOException {
        Map<String, Object> attributes;
        if (sendAttrs) {
            attributes = getAttributes(f, options);
        } else {
            attributes = Collections.emptyMap();
        }
        return getLongName(f, shortName, attributes);
    }

    protected String getLongName(Path f, String shortName, Map<String, ?> attributes) throws IOException {
        return SftpHelper.getLongName(shortName, attributes);
    }

    protected String getShortName(Path f) throws IOException {
        Path nrm = normalize(f);
        int count = nrm.getNameCount();
        /*
         * According to the javadoc:
         *
         *      The number of elements in the path, or 0 if this path only
         *      represents a root component
         */
        if (OsUtils.isUNIX()) {
            Path name = f.getFileName();
            if (name == null) {
                Path p = resolveFile(".");
                name = p.getFileName();
            }

            if (name == null) {
                if (count > 0) {
                    name = nrm.getFileName();
                }
            }

            if (name != null) {
                return name.toString();
            } else {
                return nrm.toString();
            }
        } else {    // need special handling for Windows root drives
            if (count > 0) {
                Path name = nrm.getFileName();
                return name.toString();
            } else {
                return nrm.toString().replace(File.separatorChar, '/');
            }
        }
    }

    protected Map<String, Object> resolveFileAttributes(Path file, int flags, LinkOption... options) throws IOException {
        Boolean status = IoUtils.checkFileExists(file, options);
        if (status == null) {
            return handleUnknownStatusFileAttributes(file, flags, options);
        } else if (!status) {
            throw new FileNotFoundException(file.toString());
        } else {
            return getAttributes(file, flags, options);
        }
    }

    protected void writeAttrs(Buffer buffer, Map<String, ?> attributes) throws IOException {
        SftpHelper.writeAttrs(buffer, getVersion(), attributes);
    }

    protected Map<String, Object> getAttributes(Path file, LinkOption... options) throws IOException {
        return getAttributes(file, SftpConstants.SSH_FILEXFER_ATTR_ALL, options);
    }

    protected Map<String, Object> handleUnknownStatusFileAttributes(Path file, int flags, LinkOption... options) throws IOException {
        switch (unsupportedAttributePolicy) {
            case Ignore:
                break;
            case ThrowException:
                throw new AccessDeniedException("Cannot determine existence for attributes of " + file);
            case Warn:
                break;
            default:
                break;
        }

        return getAttributes(file, flags, options);
    }

    /**
     * @param file    The {@link Path} location for the required attributes
     * @param flags   A mask of the original required attributes - ignored by the
     *                default implementation
     * @param options The {@link LinkOption}s to use in order to access the file
     *                if necessary
     * @return A {@link Map} of the retrieved attributes
     * @throws IOException If failed to access the file
     * @see #resolveMissingFileAttributes(Path, int, Map, LinkOption...)
     */
    protected Map<String, Object> getAttributes(Path file, int flags, LinkOption... options) throws IOException {
        FileSystem fs = file.getFileSystem();
        Collection<String> supportedViews = fs.supportedFileAttributeViews();
        Map<String, Object> attrs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Collection<String> views;

        if (GenericUtils.isEmpty(supportedViews)) {
            views = Collections.emptyList();
        } else if (supportedViews.contains("unix")) {
            views = DEFAULT_UNIX_VIEW;
        } else {
            views = GenericUtils.map(supportedViews, v -> v + ":*");
        }

        for (String v : views) {
            Map<String, Object> ta = readFileAttributes(file, v, options);
            if (GenericUtils.isNotEmpty(ta)) {
                attrs.putAll(ta);
            }
        }

        Map<String, Object> completions = resolveMissingFileAttributes(file, flags, attrs, options);
        if (GenericUtils.isNotEmpty(completions)) {
            attrs.putAll(completions);
        }

        return attrs;
    }

    /**
     * Called by {@link #getAttributes(Path, int, LinkOption...)} in order
     * to complete any attributes that could not be retrieved via the supported
     * file system views. These attributes are deemed important so an extra
     * effort is made to provide a value for them
     *
     * @param file    The {@link Path} location for the required attributes
     * @param flags   A mask of the original required attributes - ignored by the
     *                default implementation
     * @param current The {@link Map} of attributes already retrieved - may be
     *                {@code null}/empty and/or unmodifiable
     * @param options The {@link LinkOption}s to use in order to access the file
     *                if necessary
     * @return A {@link Map} of the extra attributes whose values need to be
     * updated in the original map. <B>Note:</B> it is allowed to specify values
     * which <U>override</U> existing ones - the default implementation does not
     * override values that have a non-{@code null} value
     * @throws IOException If failed to access the attributes - in which case
     *                     an <U>error</U> is returned to the SFTP client
     * @see #FILEATTRS_RESOLVERS
     */
    protected Map<String, Object> resolveMissingFileAttributes(Path file, int flags, Map<String, Object> current, LinkOption... options) throws IOException {
        Map<String, Object> attrs = null;
        // Cannot use forEach because the attrs variable is not effectively final
        for (Map.Entry<String, FileInfoExtractor<?>> re : FILEATTRS_RESOLVERS.entrySet()) {
            String name = re.getKey();
            Object value = GenericUtils.isEmpty(current) ? null : current.get(name);
            FileInfoExtractor<?> x = re.getValue();
            try {
                Object resolved = resolveMissingFileAttributeValue(file, name, value, x, options);
                if (Objects.equals(resolved, value)) {
                    continue;
                }

                if (attrs == null) {
                    attrs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                }
                attrs.put(name, resolved);
            } catch (IOException e) {
            }
        }

        if (attrs == null) {
            return Collections.emptyMap();
        } else {
            return attrs;
        }
    }

    protected Object resolveMissingFileAttributeValue(Path file, String name, Object value, FileInfoExtractor<?> x, LinkOption... options) throws IOException {
        if (value != null) {
            return value;
        } else {
            return x.infoOf(file, options);
        }
    }

    protected Map<String, Object> addMissingAttribute(Path file, Map<String, Object> current, String name, FileInfoExtractor<?> x, LinkOption... options) throws IOException {
        Object value = GenericUtils.isEmpty(current) ? null : current.get(name);
        if (value != null) {    // already have the value
            return current;
        }

        // skip if still no value
        value = x.infoOf(file, options);
        if (value == null) {
            return current;
        }

        if (current == null) {
            current = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        }

        current.put(name, value);
        return current;
    }

    protected Map<String, Object> readFileAttributes(Path file, String view, LinkOption... options) throws IOException {
        try {
            return Files.readAttributes(file, view, options);
        } catch (IOException e) {
            return handleReadFileAttributesException(file, view, options, e);
        }
    }

    protected Map<String, Object> handleReadFileAttributesException(Path file, String view, LinkOption[] options, IOException e) throws IOException {

        switch (unsupportedAttributePolicy) {
            case Ignore:
                break;
            case Warn:
                break;
            case ThrowException:
                throw e;
            default:
                break;
        }

        return Collections.emptyMap();
    }

    protected void doSetAttributes(Path file, Map<String, ?> attributes) throws IOException {
        SftpEventListener listener = getSftpEventListenerProxy();
        ServerSession session = getServerSession();
        listener.modifyingAttributes(session, file, attributes);
        try {
            setFileAttributes(file, attributes, IoUtils.getLinkOptions(false));
        } catch (IOException | RuntimeException e) {
            listener.modifiedAttributes(session, file, attributes, e);
            throw e;
        }
        listener.modifiedAttributes(session, file, attributes, null);
    }

    protected LinkOption[] getPathResolutionLinkOption(int cmd, String extension, Path path) throws IOException {
        ServerSession session = getServerSession();
        boolean followLinks = PropertyResolverUtils.getBooleanProperty(session, AUTO_FOLLOW_LINKS, DEFAULT_AUTO_FOLLOW_LINKS);
        return IoUtils.getLinkOptions(followLinks);
    }

    protected void setFileAttributes(Path file, Map<String, ?> attributes, LinkOption... options) throws IOException {
        Set<String> unsupported = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        // Cannot use forEach because of the potential IOException being thrown
        for (Map.Entry<String, ?> ae : attributes.entrySet()) {
            String attribute = ae.getKey();
            Object value = ae.getValue();
            String view = null;
            switch (attribute) {
                case "size": {
                    long newSize = ((Number) value).longValue();
                    SftpFileSystemAccessor accessor = getFileSystemAccessor();
                    try (SeekableByteChannel channel = accessor.openFile(getServerSession(), this, file, null, EnumSet.of(StandardOpenOption.WRITE))) {
                        channel.truncate(newSize);
                    }
                    continue;
                }
                case "uid":
                    view = "unix";
                    break;
                case "gid":
                    view = "unix";
                    break;
                case "owner":
                    view = "posix";
                    value = toUser(file, (UserPrincipal) value);
                    break;
                case "group":
                    view = "posix";
                    value = toGroup(file, (GroupPrincipal) value);
                    break;
                case "permissions":
                    view = "posix";
                    break;
                case "acl":
                    view = "acl";
                    break;
                case "creationTime":
                    view = "basic";
                    break;
                case "lastModifiedTime":
                    view = "basic";
                    break;
                case "lastAccessTime":
                    view = "basic";
                    break;
                case "extended":
                    view = "extended";
                    break;
                default:    // ignored
            }
            if ((GenericUtils.length(view) > 0) && (value != null)) {
                try {
                    setFileAttribute(file, view, attribute, value, options);
                } catch (Exception e) {
                    handleSetFileAttributeFailure(file, view, attribute, value, unsupported, e);
                }
            }
        }

        handleUnsupportedAttributes(unsupported);
    }

    protected void handleSetFileAttributeFailure(Path file, String view, String attribute, Object value, Collection<String> unsupported, Exception e) throws IOException {
        if (e instanceof UnsupportedOperationException) {
            unsupported.add(attribute);
        } else {
            if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw new IOException(e);
            }
        }
    }

    protected void setFileAttribute(Path file, String view, String attribute, Object value, LinkOption... options) throws IOException {
        if ("acl".equalsIgnoreCase(attribute) && "acl".equalsIgnoreCase(view)) {
            @SuppressWarnings("unchecked")
            List<AclEntry> acl = (List<AclEntry>) value;
            setFileAccessControl(file, acl, options);
        } else if ("permissions".equalsIgnoreCase(attribute)) {
            @SuppressWarnings("unchecked")
            Set<PosixFilePermission> perms = (Set<PosixFilePermission>) value;
            setFilePermissions(file, perms, options);
        } else if ("owner".equalsIgnoreCase(attribute) || "group".equalsIgnoreCase(attribute)) {
            setFileOwnership(file, attribute, (Principal) value, options);
        } else if ("creationTime".equalsIgnoreCase(attribute) || "lastModifiedTime".equalsIgnoreCase(attribute) || "lastAccessTime".equalsIgnoreCase(attribute)) {
            setFileTime(file, view, attribute, (FileTime) value, options);
        } else if ("extended".equalsIgnoreCase(view) && "extended".equalsIgnoreCase(attribute)) {
            @SuppressWarnings("unchecked")
            Map<String, byte[]> extensions = (Map<String, byte[]>) value;
            setFileExtensions(file, extensions, options);
        } else {
            Files.setAttribute(file, view + ":" + attribute, value, options);
        }
    }

    protected void setFileTime(Path file, String view, String attribute, FileTime value, LinkOption... options) throws IOException {
        if (value == null) {
            return;
        }
        Files.setAttribute(file, view + ":" + attribute, value, options);
    }

    protected void setFileOwnership(Path file, String attribute, Principal value, LinkOption... options) throws IOException {
        if (value == null) {
            return;
        }

        /*
         * Quoting from Javadoc of FileOwnerAttributeView#setOwner:
         *
         *      To ensure consistent and correct behavior across platforms
         *      it is recommended that this method should only be used
         *      to set the file owner to a user principal that is not a group.
         */
        if ("owner".equalsIgnoreCase(attribute)) {
            FileOwnerAttributeView view = Files.getFileAttributeView(file, FileOwnerAttributeView.class, options);
            if (view == null) {
                throw new UnsupportedOperationException("Owner view not supported for " + file);
            }

            if (!(value instanceof UserPrincipal)) {
                throw new StreamCorruptedException("Owner is not " + UserPrincipal.class.getSimpleName() + ": " + value.getClass().getSimpleName());
            }

            view.setOwner((UserPrincipal) value);
        } else if ("group".equalsIgnoreCase(attribute)) {
            PosixFileAttributeView view = Files.getFileAttributeView(file, PosixFileAttributeView.class, options);
            if (view == null) {
                throw new UnsupportedOperationException("POSIX view not supported");
            }

            if (!(value instanceof GroupPrincipal)) {
                throw new StreamCorruptedException("Group is not " + GroupPrincipal.class.getSimpleName() + ": " + value.getClass().getSimpleName());
            }

            view.setGroup((GroupPrincipal) value);
        } else {
            throw new UnsupportedOperationException("Unknown ownership attribute: " + attribute);
        }
    }

    protected void setFileExtensions(Path file, Map<String, byte[]> extensions, LinkOption... options) throws IOException {
        if (GenericUtils.isEmpty(extensions)) {
            return;
        }

        /* According to v3,4,5:
         *
         *      Implementations SHOULD ignore extended data fields that they do not understand.
         *
         * But according to v6 (https://tools.ietf.org/html/draft-ietf-secsh-filexfer-13#page-28):
         *      Implementations MUST return SSH_FX_UNSUPPORTED if there are any unrecognized extensions.
         */
        if (version < SftpConstants.SFTP_V6) {
        } else {
            throw new UnsupportedOperationException("File extensions not supported");
        }
    }

    protected void setFilePermissions(Path file, Set<PosixFilePermission> perms, LinkOption... options) throws IOException {
        if (OsUtils.isWin32()) {
            IoUtils.setPermissionsToFile(file.toFile(), perms);
            return;
        }

        PosixFileAttributeView view = Files.getFileAttributeView(file, PosixFileAttributeView.class, options);
        if (view == null) {
            throw new UnsupportedOperationException("POSIX view not supported for " + file);
        }

        view.setPermissions(perms);
    }

    protected void setFileAccessControl(Path file, List<AclEntry> acl, LinkOption... options) throws IOException {
        AclFileAttributeView view = Files.getFileAttributeView(file, AclFileAttributeView.class, options);
        if (view == null) {
            throw new UnsupportedOperationException("ACL view not supported for " + file);
        }

        view.setAcl(acl);
    }

    protected void handleUnsupportedAttributes(Collection<String> attributes) {
        if (attributes.isEmpty()) {
            return;
        }

        String attrsList = GenericUtils.join(attributes, ',');
        switch (unsupportedAttributePolicy) {
            case Ignore:
                break;
            case Warn:
                break;
            case ThrowException:
                throw new UnsupportedOperationException("Unsupported attributes: " + attrsList);
            default:
                break;
        }
    }

    protected GroupPrincipal toGroup(Path file, GroupPrincipal name) throws IOException {
        String groupName = name.toString();
        FileSystem fileSystem = file.getFileSystem();
        UserPrincipalLookupService lookupService = fileSystem.getUserPrincipalLookupService();
        try {
            if (lookupService == null) {
                throw new UserPrincipalNotFoundException(groupName);
            }
            return lookupService.lookupPrincipalByGroupName(groupName);
        } catch (IOException e) {
            handleUserPrincipalLookupServiceException(GroupPrincipal.class, groupName, e);
            return null;
        }
    }

    protected UserPrincipal toUser(Path file, UserPrincipal name) throws IOException {
        String username = name.toString();
        FileSystem fileSystem = file.getFileSystem();
        UserPrincipalLookupService lookupService = fileSystem.getUserPrincipalLookupService();
        try {
            if (lookupService == null) {
                throw new UserPrincipalNotFoundException(username);
            }
            return lookupService.lookupPrincipalByName(username);
        } catch (IOException e) {
            handleUserPrincipalLookupServiceException(UserPrincipal.class, username, e);
            return null;
        }
    }

    protected void handleUserPrincipalLookupServiceException(Class<? extends Principal> principalType, String name, IOException e) throws IOException {

        /* According to Javadoc:
         *
         *      "Where an implementation does not support any notion of group
         *      or user then this method always throws UserPrincipalNotFoundException."
         */
        switch (unsupportedAttributePolicy) {
            case Ignore:
                break;
            case Warn:
                break;
            case ThrowException:
                throw e;
            default:
                break;
        }
    }

    protected Map<String, Object> readAttrs(Buffer buffer) throws IOException {
        return SftpHelper.readAttrs(buffer, getVersion());
    }

    /**
     * Makes sure that the local handle is not null and of the specified type
     *
     * @param <H>    The generic handle type
     * @param handle The original handle id
     * @param h      The resolved {@link Handle} instance
     * @param type   The expected handle type
     * @return The cast type
     * @throws IOException            If a generic exception occurred
     * @throws FileNotFoundException  If the handle instance is {@code null}
     * @throws InvalidHandleException If the handle instance is not of the expected type
     */
    protected <H extends Handle> H validateHandle(String handle, Handle h, Class<H> type) throws IOException {
        if (h == null) {
            throw new FileNotFoundException("No such current handle: " + handle);
        }

        Class<?> t = h.getClass();
        if (!type.isAssignableFrom(t)) {
            throw new InvalidHandleException(handle, h, type);
        }

        return type.cast(h);
    }

    protected void sendStatus(Buffer buffer, int id, Throwable e) throws IOException {
        int substatus = resolveSubstatus(e);
        sendStatus(buffer, id, substatus, SftpHelper.resolveStatusMessage(e));
    }

    protected void sendStatus(Buffer buffer, int id, int substatus, String msg) throws IOException {
        sendStatus(buffer, id, substatus, (msg != null) ? msg : "", "");
    }

    protected void sendStatus(Buffer buffer, int id, int substatus, String msg, String lang) throws IOException {
        buffer.putByte((byte) SftpConstants.SSH_FXP_STATUS);
        buffer.putInt(id);
        buffer.putInt(substatus);
        buffer.putString(msg);
        buffer.putString(lang);
        send(buffer);
    }

    protected void send(Buffer buffer) throws IOException {
        int len = buffer.available();
        BufferUtils.writeInt(out, len, workBuf, 0, workBuf.length);
        out.write(buffer.array(), buffer.rpos(), len);
        out.flush();
    }

    @Override
    public void destroy() {
        if (closed.getAndSet(true)) {
            return; // ignore if already closed
        }

        ServerSession session = getServerSession();

        try {
            SftpEventListener listener = getSftpEventListenerProxy();
            listener.destroying(session);
        } catch (Exception e) {
        }

        // if thread has not completed, cancel it
        if ((pendingFuture != null) && (!pendingFuture.isDone())) {
            boolean result = pendingFuture.cancel(true);
            // TODO consider waiting some reasonable (?) amount of time for cancellation
        }

        pendingFuture = null;

        if ((executors != null) && (!executors.isShutdown()) && shutdownExecutor) {
            Collection<Runnable> runners = executors.shutdownNow();
        }

        executors = null;

        try {
            fileSystem.close();
        } catch (UnsupportedOperationException e) {
        } catch (IOException e) {
        }
    }

    protected Path resolveNormalizedLocation(String remotePath) throws IOException, InvalidPathException {
        return normalize(resolveFile(remotePath));
    }

    protected Path normalize(Path f) {
        if (f == null) {
            return null;
        }

        Path abs = f.isAbsolute() ? f : f.toAbsolutePath();
        return abs.normalize();
    }

    /**
     * @param remotePath The remote path - separated by '/'
     * @return The local {@link Path}
     * @throws IOException          If failed to resolve the local path
     * @throws InvalidPathException If bad local path specification
     */
    protected Path resolveFile(String remotePath) throws IOException, InvalidPathException {
        String path = SelectorUtils.translateToLocalFileSystemPath(remotePath, '/', defaultDir.getFileSystem());
        Path p = defaultDir.resolve(path);
        return p;
    }
}

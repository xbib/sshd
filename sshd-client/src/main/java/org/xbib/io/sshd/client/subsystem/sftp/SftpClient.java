package org.xbib.io.sshd.client.subsystem.sftp;

import org.xbib.io.sshd.client.subsystem.SubsystemClient;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.SftpClientExtension;
import org.xbib.io.sshd.common.subsystem.sftp.SftpConstants;
import org.xbib.io.sshd.common.subsystem.sftp.SftpHelper;
import org.xbib.io.sshd.common.subsystem.sftp.SftpUniversalOwnerAndGroup;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.BufferUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 */
public interface SftpClient extends SubsystemClient {
    DirEntry[] EMPTY_DIR_ENTRIES = new DirEntry[0];
    // default values used if none specified
    int MIN_BUFFER_SIZE = Byte.MAX_VALUE;
    int MIN_READ_BUFFER_SIZE = MIN_BUFFER_SIZE;
    int MIN_WRITE_BUFFER_SIZE = MIN_BUFFER_SIZE;
    int IO_BUFFER_SIZE = 32 * 1024;
    // CHECKSTYLE:ON
    int DEFAULT_READ_BUFFER_SIZE = IO_BUFFER_SIZE;
    int DEFAULT_WRITE_BUFFER_SIZE = IO_BUFFER_SIZE;
    long DEFAULT_WAIT_TIMEOUT = TimeUnit.SECONDS.toMillis(15L);
    /**
     * Property that can be used on the {@link org.xbib.io.sshd.common.FactoryManager}
     * to control the internal timeout used by the client to open a channel.
     * If not specified then {@link #DEFAULT_CHANNEL_OPEN_TIMEOUT} value
     * is used
     */
    String SFTP_CHANNEL_OPEN_TIMEOUT = "sftp-channel-open-timeout";
    long DEFAULT_CHANNEL_OPEN_TIMEOUT = DEFAULT_WAIT_TIMEOUT;
    /**
     * Default modes for opening a channel if no specific modes specified
     */
    Set<OpenMode> DEFAULT_CHANNEL_MODES =
            Collections.unmodifiableSet(EnumSet.of(OpenMode.Read, OpenMode.Write));

    /**
     * @return The negotiated SFTP protocol version
     */
    int getVersion();

    @Override
    default String getName() {
        return SftpConstants.SFTP_SUBSYSTEM_NAME;
    }

    /**
     * @return An (unmodifiable) {@link Map} of the reported server extensions.
     */
    Map<String, byte[]> getServerExtensions();

    boolean isClosing();

    /**
     * Opens a remote file for read
     *
     * @param path The remote path
     * @return The file's {@link CloseableHandle}
     * @throws IOException If failed to open the remote file
     * @see #open(String, Collection)
     */
    default CloseableHandle open(String path) throws IOException {
        return open(path, Collections.emptySet());
    }

    /**
     * Opens a remote file with the specified mode(s)
     *
     * @param path    The remote path
     * @param options The desired mode - if none specified
     *                then {@link OpenMode#Read} is assumed
     * @return The file's {@link CloseableHandle}
     * @throws IOException If failed to open the remote file
     * @see #open(String, Collection)
     */
    default CloseableHandle open(String path, OpenMode... options) throws IOException {
        return open(path, GenericUtils.of(options));
    }

    /**
     * Opens a remote file with the specified mode(s)
     *
     * @param path    The remote path
     * @param options The desired mode - if none specified
     *                then {@link OpenMode#Read} is assumed
     * @return The file's {@link CloseableHandle}
     * @throws IOException If failed to open the remote file
     */
    CloseableHandle open(String path, Collection<OpenMode> options) throws IOException;

    /**
     * Close the handle obtained from one of the {@code open} methods
     *
     * @param handle The {@code Handle} to close
     * @throws IOException If failed to execute
     */
    void close(Handle handle) throws IOException;

    /**
     * @param path The remote path to remove
     * @throws IOException If failed to execute
     */
    void remove(String path) throws IOException;

    default void rename(String oldPath, String newPath) throws IOException {
        rename(oldPath, newPath, Collections.emptySet());
    }

    default void rename(String oldPath, String newPath, CopyMode... options) throws IOException {
        rename(oldPath, newPath, GenericUtils.of(options));
    }

    //
    // Low level API
    //

    void rename(String oldPath, String newPath, Collection<CopyMode> options) throws IOException;

    /**
     * Reads data from the open (file) handle
     *
     * @param handle     The file {@link Handle} to read from
     * @param fileOffset The file offset to read from
     * @param dst        The destination buffer
     * @return Number of read bytes - {@code -1} if EOF reached
     * @throws IOException If failed to read the data
     * @see #read(Handle, long, byte[], int, int)
     */
    default int read(Handle handle, long fileOffset, byte[] dst) throws IOException {
        return read(handle, fileOffset, dst, null);
    }

    /**
     * Reads data from the open (file) handle
     *
     * @param handle       The file {@link Handle} to read from
     * @param fileOffset   The file offset to read from
     * @param dst          The destination buffer
     * @param eofSignalled If not {@code null} then upon return holds a value indicating
     *                     whether EOF was reached due to the read. If {@code null} indicator
     *                     value then this indication is not available
     * @return Number of read bytes - {@code -1} if EOF reached
     * @throws IOException If failed to read the data
     * @see #read(Handle, long, byte[], int, int, AtomicReference)
     * @see <A HREF="https://tools.ietf.org/html/draft-ietf-secsh-filexfer-13#section-9.3">SFTP v6 - section 9.3</A>
     */
    default int read(Handle handle, long fileOffset, byte[] dst, AtomicReference<Boolean> eofSignalled) throws IOException {
        return read(handle, fileOffset, dst, 0, dst.length, eofSignalled);
    }

    default int read(Handle handle, long fileOffset, byte[] dst, int dstOffset, int len) throws IOException {
        return read(handle, fileOffset, dst, dstOffset, len, null);
    }

    /**
     * Reads data from the open (file) handle
     *
     * @param handle       The file {@link Handle} to read from
     * @param fileOffset   The file offset to read from
     * @param dst          The destination buffer
     * @param dstOffset    Offset in destination buffer to place the read data
     * @param len          Available destination buffer size to read
     * @param eofSignalled If not {@code null} then upon return holds a value indicating
     *                     whether EOF was reached due to the read. If {@code null} indicator
     *                     value then this indication is not available
     * @return Number of read bytes - {@code -1} if EOF reached
     * @throws IOException If failed to read the data
     * @see <A HREF="https://tools.ietf.org/html/draft-ietf-secsh-filexfer-13#section-9.3">SFTP v6 - section 9.3</A>
     */
    int read(Handle handle, long fileOffset, byte[] dst, int dstOffset, int len, AtomicReference<Boolean> eofSignalled) throws IOException;

    default void write(Handle handle, long fileOffset, byte[] src) throws IOException {
        write(handle, fileOffset, src, 0, src.length);
    }

    /**
     * Write data to (open) file handle
     *
     * @param handle     The file {@link Handle}
     * @param fileOffset Zero-based offset to write in file
     * @param src        Data buffer
     * @param srcOffset  Offset of valid data in buffer
     * @param len        Number of bytes to write
     * @throws IOException If failed to write the data
     */
    void write(Handle handle, long fileOffset, byte[] src, int srcOffset, int len) throws IOException;

    /**
     * Create remote directory
     *
     * @param path Remote directory path
     * @throws IOException If failed to execute
     */
    void mkdir(String path) throws IOException;

    /**
     * Remove remote directory
     *
     * @param path Remote directory path
     * @throws IOException If failed to execute
     */
    void rmdir(String path) throws IOException;

    /**
     * Obtain a handle for a directory
     *
     * @param path Remote directory path
     * @return The associated directory {@link Handle}
     * @throws IOException If failed to execute
     */
    CloseableHandle openDir(String path) throws IOException;

    /**
     * @param handle Directory {@link Handle} to read from
     * @return A {@link List} of entries - {@code null} to indicate no more entries
     * <B>Note:</B> the list may be <U>incomplete</U> since the client and
     * server have some internal imposed limit on the number of entries they
     * can process. Therefore several calls to this method may be required
     * (until {@code null}). In order to iterate over all the entries use
     * {@link #readDir(String)}
     * @throws IOException If failed to access the remote site
     */
    default List<DirEntry> readDir(Handle handle) throws IOException {
        return readDir(handle, null);
    }

    /**
     * @param handle       Directory {@link Handle} to read from
     * @param eolIndicator An indicator that can be used to get information
     *                     whether end of list has been reached - ignored if {@code null}. Upon
     *                     return, set value indicates whether all entries have been exhausted - a {@code null}
     *                     value means that this information cannot be provided and another call to
     *                     {@code readDir} is necessary in order to verify that no more entries are pending
     * @return A {@link List} of entries - {@code null} to indicate no more entries
     * @throws IOException If failed to access the remote site
     * @see <A HREF="https://tools.ietf.org/html/draft-ietf-secsh-filexfer-13#section-9.4">SFTP v6 - section 9.4</A>
     */
    List<DirEntry> readDir(Handle handle, AtomicReference<Boolean> eolIndicator) throws IOException;

    /**
     * @param handle A directory {@link Handle}
     * @return An {@link Iterable} that can be used to iterate over all the
     * directory entries (like {@link #readDir(String)}). <B>Note:</B> the
     * iterable instance is not re-usable - i.e., files can be iterated
     * only <U>once</U>
     * @throws IOException If failed to access the directory
     */
    default Iterable<DirEntry> listDir(Handle handle) throws IOException {
        if (!isOpen()) {
            throw new IOException("listDir(" + handle + ") client is closed");
        }

        return new StfpIterableDirHandle(this, handle);
    }

    /**
     * The effective &quot;normalized&quot; remote path
     *
     * @param path The requested path - may be relative, and/or contain
     *             dots - e.g., &quot;.&quot;, &quot;..&quot;, &quot;./foo&quot;, &quot;../bar&quot;
     * @return The effective &quot;normalized&quot; remote path
     * @throws IOException If failed to execute
     */
    String canonicalPath(String path) throws IOException;

    /**
     * Retrieve remote path meta-data - follow symbolic links if encountered
     *
     * @param path The remote path
     * @return The associated {@link Attributes}
     * @throws IOException If failed to execute
     */
    Attributes stat(String path) throws IOException;

    /**
     * Retrieve remote path meta-data - do <B>not</B> follow symbolic links
     *
     * @param path The remote path
     * @return The associated {@link Attributes}
     * @throws IOException If failed to execute
     */
    Attributes lstat(String path) throws IOException;

    /**
     * Retrieve file/directory handle meta-data
     *
     * @param handle The {@link Handle} obtained via one of the {@code open} calls
     * @return The associated {@link Attributes}
     * @throws IOException If failed to execute
     */
    Attributes stat(Handle handle) throws IOException;

    /**
     * Update remote node meta-data
     *
     * @param path       The remote path
     * @param attributes The {@link Attributes} to update
     * @throws IOException If failed to execute
     */
    void setStat(String path, Attributes attributes) throws IOException;

    /**
     * Update remote node meta-data
     *
     * @param handle     The {@link Handle} obtained via one of the {@code open} calls
     * @param attributes The {@link Attributes} to update
     * @throws IOException If failed to execute
     */
    void setStat(Handle handle, Attributes attributes) throws IOException;

    /**
     * Retrieve target of a link
     *
     * @param path Remote path that represents a link
     * @return The link target
     * @throws IOException If failed to execute
     */
    String readLink(String path) throws IOException;

    /**
     * Create symbolic link
     *
     * @param linkPath   The link location
     * @param targetPath The referenced target by the link
     * @throws IOException If failed to execute
     * @see #link(String, String, boolean)
     */
    default void symLink(String linkPath, String targetPath) throws IOException {
        link(linkPath, targetPath, true);
    }

    /**
     * Create a link
     *
     * @param linkPath   The link location
     * @param targetPath The referenced target by the link
     * @param symbolic   If {@code true} then make this a symbolic link, otherwise a hard one
     * @throws IOException If failed to execute
     */
    void link(String linkPath, String targetPath, boolean symbolic) throws IOException;

    // see SSH_FXP_BLOCK / SSH_FXP_UNBLOCK for byte range locks
    void lock(Handle handle, long offset, long length, int mask) throws IOException;

    void unlock(Handle handle, long offset, long length) throws IOException;

    /**
     * @param path The remote directory path
     * @return An {@link Iterable} that can be used to iterate over all the
     * directory entries (unlike {@link #readDir(Handle)})
     * @throws IOException If failed to access the remote site
     * @see #readDir(Handle)
     */
    default Iterable<DirEntry> readDir(String path) throws IOException {
        if (!isOpen()) {
            throw new IOException("readDir(" + path + ") client is closed");
        }

        return new SftpIterableDirEntry(this, path);
    }

    default InputStream read(String path) throws IOException {
        return read(path, DEFAULT_READ_BUFFER_SIZE);
    }

    default InputStream read(String path, int bufferSize) throws IOException {
        return read(path, bufferSize, EnumSet.of(OpenMode.Read));
    }

    //
    // High level API
    //

    default InputStream read(String path, OpenMode... mode) throws IOException {
        return read(path, DEFAULT_READ_BUFFER_SIZE, mode);
    }

    default InputStream read(String path, int bufferSize, OpenMode... mode) throws IOException {
        return read(path, bufferSize, GenericUtils.of(mode));
    }

    default InputStream read(String path, Collection<OpenMode> mode) throws IOException {
        return read(path, DEFAULT_READ_BUFFER_SIZE, mode);
    }

    /**
     * Read a remote file's data via an input stream
     *
     * @param path       The remote file path
     * @param bufferSize The internal read buffer size
     * @param mode       The remote file {@link OpenMode}s
     * @return An {@link InputStream} for reading the remote file data
     * @throws IOException If failed to execute
     */
    default InputStream read(String path, int bufferSize, Collection<OpenMode> mode) throws IOException {
        if (bufferSize < MIN_READ_BUFFER_SIZE) {
            throw new IllegalArgumentException("Insufficient read buffer size: " + bufferSize + ", min.=" + MIN_READ_BUFFER_SIZE);
        }

        if (!isOpen()) {
            throw new IOException("read(" + path + ")[" + mode + "] size=" + bufferSize + ": client is closed");
        }

        return new SftpInputStreamWithChannel(this, bufferSize, path, mode);
    }

    default OutputStream write(String path) throws IOException {
        return write(path, DEFAULT_WRITE_BUFFER_SIZE);
    }

    default OutputStream write(String path, int bufferSize) throws IOException {
        return write(path, bufferSize, EnumSet.of(OpenMode.Write, OpenMode.Create, OpenMode.Truncate));
    }

    default OutputStream write(String path, OpenMode... mode) throws IOException {
        return write(path, DEFAULT_WRITE_BUFFER_SIZE, mode);
    }

    default OutputStream write(String path, int bufferSize, OpenMode... mode) throws IOException {
        return write(path, bufferSize, GenericUtils.of(mode));
    }

    default OutputStream write(String path, Collection<OpenMode> mode) throws IOException {
        return write(path, DEFAULT_WRITE_BUFFER_SIZE, mode);
    }

    /**
     * Write to a remote file via an output stream
     *
     * @param path       The remote file path
     * @param bufferSize The internal write buffer size
     * @param mode       The remote file {@link OpenMode}s
     * @return An {@link OutputStream} for writing the data
     * @throws IOException If failed to execute
     */
    default OutputStream write(String path, int bufferSize, Collection<OpenMode> mode) throws IOException {
        if (bufferSize < MIN_WRITE_BUFFER_SIZE) {
            throw new IllegalArgumentException("Insufficient write buffer size: " + bufferSize + ", min.=" + MIN_WRITE_BUFFER_SIZE);
        }

        if (!isOpen()) {
            throw new IOException("write(" + path + ")[" + mode + "] size=" + bufferSize + ": client is closed");
        }

        return new SftpOutputStreamWithChannel(this, bufferSize, path, mode);
    }

    /**
     * @param <E>           The generic extension type
     * @param extensionType The extension type
     * @return The extension instance - <B>Note:</B> it is up to the caller
     * to invoke {@link SftpClientExtension#isSupported()} - {@code null} if
     * this extension type is not implemented by the client
     * @see #getServerExtensions()
     */
    <E extends SftpClientExtension> E getExtension(Class<? extends E> extensionType);

    /**
     * @param extensionName The extension name
     * @return The extension instance - <B>Note:</B> it is up to the caller
     * to invoke {@link SftpClientExtension#isSupported()} - {@code null} if
     * this extension type is not implemented by the client
     * @see #getServerExtensions()
     */
    SftpClientExtension getExtension(String extensionName);

    enum OpenMode {
        Read,
        Write,
        Append,
        Create,
        Truncate,
        Exclusive;

        /**
         * The {@link Set} of {@link OpenOption}-s supported by {@link #fromOpenOptions(Collection)}
         */
        public static final Set<OpenOption> SUPPORTED_OPTIONS =
                Collections.unmodifiableSet(
                        EnumSet.of(
                                StandardOpenOption.READ, StandardOpenOption.APPEND,
                                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                                StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW,
                                StandardOpenOption.SPARSE));

        /**
         * Converts {@link StandardOpenOption}-s into {@link OpenMode}-s
         *
         * @param options The original options - ignored if {@code null}/empty
         * @return A {@link Set} of the equivalent modes
         * @throws IllegalArgumentException If an unsupported option is requested
         * @see #SUPPORTED_OPTIONS
         */
        public static Set<OpenMode> fromOpenOptions(Collection<? extends OpenOption> options) {
            if (GenericUtils.isEmpty(options)) {
                return Collections.emptySet();
            }

            Set<OpenMode> modes = EnumSet.noneOf(OpenMode.class);
            for (OpenOption option : options) {
                if (option == StandardOpenOption.READ) {
                    modes.add(Read);
                } else if (option == StandardOpenOption.APPEND) {
                    modes.add(Append);
                } else if (option == StandardOpenOption.CREATE) {
                    modes.add(Create);
                } else if (option == StandardOpenOption.TRUNCATE_EXISTING) {
                    modes.add(Truncate);
                } else if (option == StandardOpenOption.WRITE) {
                    modes.add(Write);
                } else if (option == StandardOpenOption.CREATE_NEW) {
                    modes.add(Create);
                    modes.add(Exclusive);
                } else if (option == StandardOpenOption.SPARSE) {
                    /*
                     * As per the Javadoc:
                     *
                     *      The option is ignored when the file system does not
                     *  support the creation of sparse files
                     */
                    continue;
                } else {
                    throw new IllegalArgumentException("Unsupported open option: " + option);
                }
            }

            return modes;
        }
    }

    enum CopyMode {
        Atomic,
        Overwrite
    }

    enum Attribute {
        Size,
        UidGid,
        Perms,
        OwnerGroup,
        AccessTime,
        ModifyTime,
        CreateTime,
        Acl,
        Extensions
    }

    class Handle {
        private final String path;
        private final byte[] id;

        Handle(String path, byte[] id) {
            // clone the original so the handle is immutable
            this.path = ValidateUtils.checkNotNullAndNotEmpty(path, "No remote path");
            this.id = ValidateUtils.checkNotNullAndNotEmpty(id, "No handle ID").clone();
        }

        /**
         * @return The remote path represented by this handle
         */
        public String getPath() {
            return path;
        }

        public int length() {
            return id.length;
        }

        /**
         * @return A <U>cloned</U> instance of the identifier in order to
         * avoid inadvertent modifications to the handle contents
         */
        public byte[] getIdentifier() {
            return id.clone();
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(id);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (obj == this) {
                return true;
            }

            // we do not ask getClass() == obj.getClass() in order to allow for derived classes equality
            if (!(obj instanceof Handle)) {
                return false;
            }

            return Arrays.equals(id, ((Handle) obj).id);
        }

        @Override
        public String toString() {
            return getPath() + ": " + BufferUtils.toHex(BufferUtils.EMPTY_HEX_SEPARATOR, id);
        }
    }

    // CHECKSTYLE:OFF
    abstract class CloseableHandle extends Handle implements Channel, Closeable {
        protected CloseableHandle(String path, byte[] id) {
            super(path, id);
        }
    }

    class Attributes {
        private Set<Attribute> flags = EnumSet.noneOf(Attribute.class);
        private int type = SftpConstants.SSH_FILEXFER_TYPE_UNKNOWN;
        private int perms;
        private int uid;
        private int gid;
        private String owner;
        private String group;
        private long size;
        private FileTime accessTime;
        private FileTime createTime;
        private FileTime modifyTime;
        private List<AclEntry> acl;
        private Map<String, byte[]> extensions = Collections.emptyMap();

        public Attributes() {
            super();
        }

        public Set<Attribute> getFlags() {
            return flags;
        }

        public Attributes addFlag(Attribute flag) {
            flags.add(flag);
            return this;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
            addFlag(Attribute.Size);
        }

        public Attributes size(long size) {
            setSize(size);
            return this;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = ValidateUtils.checkNotNullAndNotEmpty(owner, "No owner");
            addFlag(Attribute.OwnerGroup);
            if (GenericUtils.isEmpty(getGroup())) {
                setGroup(SftpUniversalOwnerAndGroup.Group.getName());
            }
        }

        public Attributes owner(String owner) {
            setOwner(owner);
            return this;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = ValidateUtils.checkNotNullAndNotEmpty(group, "No group");
            addFlag(Attribute.OwnerGroup);
            if (GenericUtils.isEmpty(getOwner())) {
                setOwner(SftpUniversalOwnerAndGroup.Owner.getName());
            }
        }

        public Attributes group(String group) {
            setGroup(group);
            return this;
        }

        public int getUserId() {
            return uid;
        }

        public int getGroupId() {
            return gid;
        }

        public Attributes owner(int uid, int gid) {
            this.uid = uid;
            this.gid = gid;
            addFlag(Attribute.UidGid);
            return this;
        }

        public int getPermissions() {
            return perms;
        }

        public void setPermissions(int perms) {
            this.perms = perms;
            addFlag(Attribute.Perms);
        }

        public Attributes perms(int perms) {
            setPermissions(perms);
            return this;
        }

        public FileTime getAccessTime() {
            return accessTime;
        }

        public void setAccessTime(FileTime atime) {
            accessTime = Objects.requireNonNull(atime, "No access time");
            addFlag(Attribute.AccessTime);
        }

        public Attributes accessTime(long atime) {
            return accessTime(atime, TimeUnit.SECONDS);
        }

        public Attributes accessTime(long atime, TimeUnit unit) {
            return accessTime(FileTime.from(atime, unit));
        }

        public Attributes accessTime(FileTime atime) {
            setAccessTime(atime);
            return this;
        }

        public FileTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(FileTime ctime) {
            createTime = Objects.requireNonNull(ctime, "No create time");
            addFlag(Attribute.CreateTime);
        }

        public Attributes createTime(long ctime) {
            return createTime(ctime, TimeUnit.SECONDS);
        }

        public Attributes createTime(long ctime, TimeUnit unit) {
            return createTime(FileTime.from(ctime, unit));
        }

        public Attributes createTime(FileTime ctime) {
            setCreateTime(ctime);
            return this;
        }

        public FileTime getModifyTime() {
            return modifyTime;
        }

        public void setModifyTime(FileTime mtime) {
            modifyTime = Objects.requireNonNull(mtime, "No modify time");
            addFlag(Attribute.ModifyTime);
        }

        public Attributes modifyTime(long mtime) {
            return modifyTime(mtime, TimeUnit.SECONDS);
        }

        public Attributes modifyTime(long mtime, TimeUnit unit) {
            return modifyTime(FileTime.from(mtime, unit));
        }

        public Attributes modifyTime(FileTime mtime) {
            setModifyTime(mtime);
            return this;
        }

        public List<AclEntry> getAcl() {
            return acl;
        }

        public void setAcl(List<AclEntry> acl) {
            this.acl = Objects.requireNonNull(acl, "No ACLs");
            addFlag(Attribute.Acl);
        }

        public Attributes acl(List<AclEntry> acl) {
            setAcl(acl);
            return this;
        }

        public Map<String, byte[]> getExtensions() {
            return extensions;
        }

        public void setExtensions(Map<String, byte[]> extensions) {
            this.extensions = Objects.requireNonNull(extensions, "No extensions");
            addFlag(Attribute.Extensions);
        }

        public Attributes extensions(Map<String, byte[]> extensions) {
            setExtensions(extensions);
            return this;
        }

        public void setStringExtensions(Map<String, String> extensions) {
            setExtensions(SftpHelper.toBinaryExtensions(extensions));
        }

        public boolean isRegularFile() {
            return (getPermissions() & SftpConstants.S_IFMT) == SftpConstants.S_IFREG;
        }

        public boolean isDirectory() {
            return (getPermissions() & SftpConstants.S_IFMT) == SftpConstants.S_IFDIR;
        }

        public boolean isSymbolicLink() {
            return (getPermissions() & SftpConstants.S_IFMT) == SftpConstants.S_IFLNK;
        }

        public boolean isOther() {
            return !isRegularFile() && !isDirectory() && !isSymbolicLink();
        }

        @Override
        public String toString() {
            return "type=" + getType()
                    + ";size=" + getSize()
                    + ";uid=" + getUserId()
                    + ";gid=" + getGroupId()
                    + ";perms=0x" + Integer.toHexString(getPermissions())
                    + ";flags=" + getFlags()
                    + ";owner=" + getOwner()
                    + ";group=" + getGroup()
                    + ";aTime=" + getAccessTime()
                    + ";cTime=" + getCreateTime()
                    + ";mTime=" + getModifyTime()
                    + ";extensions=" + getExtensions().keySet();
        }
    }

    class DirEntry {
        public static final Comparator<DirEntry> BY_CASE_SENSITIVE_FILENAME = new Comparator<DirEntry>() {
            @Override
            public int compare(DirEntry o1, DirEntry o2) {
                if (o1 == o2) {
                    return 0;
                } else if (o1 == null) {
                    return 1;
                } else if (o2 == null) {
                    return -1;
                } else {
                    return GenericUtils.safeCompare(o1.getFilename(), o2.getFilename(), true);
                }
            }
        };

        public static final Comparator<DirEntry> BY_CASE_INSENSITIVE_FILENAME = new Comparator<DirEntry>() {
            @Override
            public int compare(DirEntry o1, DirEntry o2) {
                if (o1 == o2) {
                    return 0;
                } else if (o1 == null) {
                    return 1;
                } else if (o2 == null) {
                    return -1;
                } else {
                    return GenericUtils.safeCompare(o1.getFilename(), o2.getFilename(), false);
                }
            }
        };

        private final String filename;
        private final String longFilename;
        private final Attributes attributes;

        DirEntry(String filename, String longFilename, Attributes attributes) {
            this.filename = filename;
            this.longFilename = longFilename;
            this.attributes = attributes;
        }

        public String getFilename() {
            return filename;
        }

        public String getLongFilename() {
            return longFilename;
        }

        public Attributes getAttributes() {
            return attributes;
        }

        @Override
        public String toString() {
            return getFilename() + "[" + getLongFilename() + "]: " + getAttributes();
        }
    }
}

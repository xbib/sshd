package org.xbib.io.sshd.common.file.virtualfs;

import org.xbib.io.sshd.common.file.FileSystemFactory;
import org.xbib.io.sshd.common.file.root.RootedFileSystemProvider;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.ValidateUtils;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSHd file system factory to reduce the visibility to a physical folder.
 */
public class VirtualFileSystemFactory implements FileSystemFactory {

    private final Map<String, Path> homeDirs = new ConcurrentHashMap<>();
    private Path defaultHomeDir;

    public VirtualFileSystemFactory() {
        super();
    }

    public VirtualFileSystemFactory(Path defaultHomeDir) {
        this.defaultHomeDir = defaultHomeDir;
    }

    public Path getDefaultHomeDir() {
        return defaultHomeDir;
    }

    public void setDefaultHomeDir(Path defaultHomeDir) {
        this.defaultHomeDir = defaultHomeDir;
    }

    public void setUserHomeDir(String userName, Path userHomeDir) {
        homeDirs.put(ValidateUtils.checkNotNullAndNotEmpty(userName, "No username"),
                Objects.requireNonNull(userHomeDir, "No home dir"));
    }

    public Path getUserHomeDir(String userName) {
        return homeDirs.get(ValidateUtils.checkNotNullAndNotEmpty(userName, "No username"));
    }

    @Override
    public FileSystem createFileSystem(Session session) throws IOException {
        String username = session.getUsername();
        Path dir = computeRootDir(session);
        if (dir == null) {
            throw new InvalidPathException(username, "Cannot resolve home directory");
        }

        return new RootedFileSystemProvider().newFileSystem(dir, Collections.emptyMap());
    }

    protected Path computeRootDir(Session session) throws IOException {
        String username = session.getUsername();
        Path homeDir = getUserHomeDir(username);
        if (homeDir == null) {
            homeDir = getDefaultHomeDir();
        }

        return homeDir;
    }
}

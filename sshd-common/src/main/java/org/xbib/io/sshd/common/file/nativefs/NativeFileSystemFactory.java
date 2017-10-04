package org.xbib.io.sshd.common.file.nativefs;

import org.xbib.io.sshd.common.file.FileSystemFactory;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Native file system factory. It uses the OS file system.
 */
public class NativeFileSystemFactory extends AbstractLoggingBean implements FileSystemFactory {
    public static final String DEFAULT_USERS_HOME = "/home";

    public static final NativeFileSystemFactory INSTANCE = new NativeFileSystemFactory();

    private boolean createHome;
    private String usersHomeDir = DEFAULT_USERS_HOME;

    public NativeFileSystemFactory() {
        this(false);
    }

    public NativeFileSystemFactory(boolean createHome) {
        this.createHome = createHome;
    }

    /**
     * @return The root location where users home is to be created - never {@code null}/empty.
     */
    public String getUsersHomeDir() {
        return usersHomeDir;
    }

    /**
     * Set the root location where users home is to be created
     *
     * @param usersHomeDir The root location where users home is to be created - never {@code null}/empty.
     * @see #isCreateHome()
     */
    public void setUsersHomeDir(String usersHomeDir) {
        this.usersHomeDir = ValidateUtils.checkNotNullAndNotEmpty(usersHomeDir, "No users home dir");
    }

    /**
     * Should the home directories be created automatically
     *
     * @return {@code true} if the file system will create the home directory if not available
     */
    public boolean isCreateHome() {
        return createHome;
    }

    /**
     * Set if the home directories be created automatically
     *
     * @param createHome {@code true} if the file system should create the home directory
     *                   automatically if not available
     * @see #getUsersHomeDir()
     */
    public void setCreateHome(boolean createHome) {
        this.createHome = createHome;
    }

    @Override
    public FileSystem createFileSystem(Session session) throws IOException {
        String userName = session.getUsername();
        // create home if does not exist
        if (isCreateHome()) {
            String homeRoot = getUsersHomeDir();
            Path homeDir = Paths.get(homeRoot, userName).normalize().toAbsolutePath();
            if (Files.exists(homeDir)) {
                if (!Files.isDirectory(homeDir)) {
                    throw new NotDirectoryException(homeDir.toString());
                }
            } else {
                Path p = Files.createDirectories(homeDir);
            }
        }

        return FileSystems.getDefault();
    }
}

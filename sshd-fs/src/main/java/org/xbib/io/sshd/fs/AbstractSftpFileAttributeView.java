package org.xbib.io.sshd.fs;

import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.common.subsystem.sftp.SftpConstants;
import org.xbib.io.sshd.common.subsystem.sftp.SftpException;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttributeView;
import java.util.Objects;

/**
 *
 */
public abstract class AbstractSftpFileAttributeView extends AbstractLoggingBean implements FileAttributeView {
    protected final SftpFileSystemProvider provider;
    protected final Path path;
    protected final LinkOption[] options;

    protected AbstractSftpFileAttributeView(SftpFileSystemProvider provider, Path path, LinkOption... options) {
        this.provider = Objects.requireNonNull(provider, "No file system provider instance");
        this.path = Objects.requireNonNull(path, "No path");
        this.options = options;
    }

    @Override
    public String name() {
        return "view";
    }

    /**
     * @return The underlying {@link SftpFileSystemProvider} used to
     * provide the view functionality
     */
    public final SftpFileSystemProvider provider() {
        return provider;
    }

    /**
     * @return The referenced view {@link Path}
     */
    public final Path getPath() {
        return path;
    }

    protected SftpClient.Attributes readRemoteAttributes() throws IOException {
        return provider.readRemoteAttributes(provider.toSftpPath(path), options);
    }

    protected void writeRemoteAttributes(SftpClient.Attributes attrs) throws IOException {
        SftpPath p = provider.toSftpPath(path);
        SftpFileSystem fs = p.getFileSystem();
        try (SftpClient client = fs.getClient()) {
            try {
                client.setStat(p.toString(), attrs);
            } catch (SftpException e) {
                if (e.getStatus() == SftpConstants.SSH_FX_NO_SUCH_FILE) {
                    throw new NoSuchFileException(p.toString());
                }
                throw e;
            }
        }
    }
}

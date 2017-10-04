package org.xbib.io.sshd.common.scp.helpers;

import org.xbib.io.sshd.common.scp.ScpFileOpener;
import org.xbib.io.sshd.common.scp.ScpSourceStreamResolver;
import org.xbib.io.sshd.common.scp.ScpTimestamp;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.io.IoUtils;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 *
 */
public class LocalFileScpSourceStreamResolver extends AbstractLoggingBean implements ScpSourceStreamResolver {
    protected final Path path;
    protected final ScpFileOpener opener;
    protected final Path name;
    protected final Set<PosixFilePermission> perms;
    protected final long size;
    protected final ScpTimestamp time;

    public LocalFileScpSourceStreamResolver(Path path, ScpFileOpener opener) throws IOException {
        this.path = Objects.requireNonNull(path, "No path specified");
        this.opener = (opener == null) ? DefaultScpFileOpener.INSTANCE : opener;
        this.name = path.getFileName();
        this.perms = IoUtils.getPermissions(path);

        BasicFileAttributes basic = Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes();
        this.size = basic.size();
        this.time = new ScpTimestamp(basic.lastModifiedTime().toMillis(), basic.lastAccessTime().toMillis());
    }

    @Override
    public String getFileName() throws IOException {
        return name.toString();
    }

    @Override
    public Collection<PosixFilePermission> getPermissions() throws IOException {
        return perms;
    }

    @Override
    public ScpTimestamp getTimestamp() throws IOException {
        return time;
    }

    @Override
    public long getSize() throws IOException {
        return size;
    }

    @Override
    public Path getEventListenerFilePath() {
        return path;
    }

    @Override
    public InputStream resolveSourceStream(Session session, OpenOption... options) throws IOException {
        return opener.openRead(session, getEventListenerFilePath(), options);
    }

    @Override
    public String toString() {
        return String.valueOf(getEventListenerFilePath());
    }
}

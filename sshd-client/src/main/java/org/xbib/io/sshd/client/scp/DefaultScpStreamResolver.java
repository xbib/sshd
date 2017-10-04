package org.xbib.io.sshd.client.scp;

import org.xbib.io.sshd.common.scp.ScpSourceStreamResolver;
import org.xbib.io.sshd.common.scp.ScpTimestamp;
import org.xbib.io.sshd.common.session.Session;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collection;

/**
 */
public class DefaultScpStreamResolver implements ScpSourceStreamResolver {
    private final String name;
    private final Path mockPath;
    private final Collection<PosixFilePermission> perms;
    private final ScpTimestamp time;
    private final long size;
    private final InputStream local;
    private final String cmd;

    public DefaultScpStreamResolver(String name, Path mockPath, Collection<PosixFilePermission> perms, ScpTimestamp time, long size, InputStream local, String cmd) {
        this.name = name;
        this.mockPath = mockPath;
        this.perms = perms;
        this.time = time;
        this.size = size;
        this.local = local;
        this.cmd = cmd;
    }

    @Override
    public String getFileName() throws IOException {
        return name;
    }

    @Override
    public Path getEventListenerFilePath() {
        return mockPath;
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
    public InputStream resolveSourceStream(Session session, OpenOption... options) throws IOException {
        return local;
    }

    @Override
    public String toString() {
        return cmd;
    }
}
package org.xbib.io.sshd.fs;

import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

/**
 */
public class SftpFileSystemChannel extends SftpRemotePathChannel {
    public SftpFileSystemChannel(SftpPath p, Collection<SftpClient.OpenMode> modes) throws IOException {
        this(Objects.requireNonNull(p, "No target path").toString(), p.getFileSystem(), modes);
    }

    public SftpFileSystemChannel(String remotePath, SftpFileSystem fs, Collection<SftpClient.OpenMode> modes) throws IOException {
        super(remotePath, Objects.requireNonNull(fs, "No SFTP file system").getClient(), true, modes);
    }
}

package org.xbib.io.sshd.fs;

import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Implements a remote {@link DirectoryStream}.
 */
public class SftpDirectoryStream implements DirectoryStream<Path> {
    private final SftpClient sftp;
    private final Iterable<SftpClient.DirEntry> iter;
    private final SftpPath p;

    /**
     * @param path The remote {@link SftpPath}
     * @throws IOException If failed to initialize the directory access handle
     */
    public SftpDirectoryStream(SftpPath path) throws IOException {
        SftpFileSystem fs = path.getFileSystem();
        p = path;
        sftp = fs.getClient();
        iter = sftp.readDir(path.toString());
    }

    /**
     * Client instance used to access the remote directory
     *
     * @return The {@link SftpClient} instance used to access the remote directory
     */
    public final SftpClient getClient() {
        return sftp;
    }

    @Override
    public Iterator<Path> iterator() {
        return new SftpPathIterator(p, iter);
    }

    @Override
    public void close() throws IOException {
        sftp.close();
    }
}

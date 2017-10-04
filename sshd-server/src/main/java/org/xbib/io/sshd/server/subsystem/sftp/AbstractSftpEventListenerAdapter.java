package org.xbib.io.sshd.server.subsystem.sftp;

import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;
import org.xbib.io.sshd.server.session.ServerSession;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

/**
 * A no-op implementation of {@link SftpEventListener} for those who wish to
 * implement only a small number of methods. By default, all non-overridden methods
 * simply log at TRACE level their invocation parameters.
 */
public abstract class AbstractSftpEventListenerAdapter extends AbstractLoggingBean implements SftpEventListener {
    protected AbstractSftpEventListenerAdapter() {
        super();
    }

    @Override
    public void initialized(ServerSession session, int version) {
    }

    @Override
    public void destroying(ServerSession session) {
    }

    @Override
    public void opening(ServerSession session, String remoteHandle, org.xbib.io.sshd.server.subsystem.sftp.Handle localHandle) throws IOException {
    }

    @Override
    public void open(ServerSession session, String remoteHandle, org.xbib.io.sshd.server.subsystem.sftp.Handle localHandle) {
    }

    @Override
    public void read(ServerSession session, String remoteHandle, DirectoryHandle localHandle, Map<String, Path> entries)
            throws IOException {
    }

    @Override
    public void reading(ServerSession session, String remoteHandle, org.xbib.io.sshd.server.subsystem.sftp.FileHandle localHandle,
                        long offset, byte[] data, int dataOffset, int dataLen)
            throws IOException {
    }

    @Override
    public void read(ServerSession session, String remoteHandle, org.xbib.io.sshd.server.subsystem.sftp.FileHandle localHandle,
                     long offset, byte[] data, int dataOffset, int dataLen, int readLen, Throwable thrown)
            throws IOException {
    }

    @Override
    public void writing(ServerSession session, String remoteHandle, org.xbib.io.sshd.server.subsystem.sftp.FileHandle localHandle,
                        long offset, byte[] data, int dataOffset, int dataLen)
            throws IOException {
    }

    @Override
    public void written(ServerSession session, String remoteHandle, org.xbib.io.sshd.server.subsystem.sftp.FileHandle localHandle,
                        long offset, byte[] data, int dataOffset, int dataLen, Throwable thrown)
            throws IOException {
    }

    @Override
    public void blocking(ServerSession session, String remoteHandle, org.xbib.io.sshd.server.subsystem.sftp.FileHandle localHandle, long offset, long length, int mask)
            throws IOException {
    }

    @Override
    public void blocked(ServerSession session, String remoteHandle, org.xbib.io.sshd.server.subsystem.sftp.FileHandle localHandle,
                        long offset, long length, int mask, Throwable thrown)
            throws IOException {
    }

    @Override
    public void unblocking(ServerSession session, String remoteHandle, org.xbib.io.sshd.server.subsystem.sftp.FileHandle localHandle, long offset, long length)
            throws IOException {
    }

    @Override
    public void unblocked(ServerSession session, String remoteHandle, FileHandle localHandle,
                          long offset, long length, Throwable thrown)
            throws IOException {
    }

    @Override
    public void close(ServerSession session, String remoteHandle, Handle localHandle) {
    }

    @Override
    public void creating(ServerSession session, Path path, Map<String, ?> attrs)
            throws IOException {
    }

    @Override
    public void created(ServerSession session, Path path, Map<String, ?> attrs, Throwable thrown)
            throws IOException {
    }

    @Override
    public void moving(ServerSession session, Path srcPath, Path dstPath, Collection<CopyOption> opts)
            throws IOException {
    }

    @Override
    public void moved(ServerSession session, Path srcPath, Path dstPath, Collection<CopyOption> opts, Throwable thrown)
            throws IOException {
    }

    @Override
    public void removing(ServerSession session, Path path)
            throws IOException {
    }

    @Override
    public void removed(ServerSession session, Path path, Throwable thrown)
            throws IOException {
    }

    @Override
    public void linking(ServerSession session, Path source, Path target, boolean symLink)
            throws IOException {
    }

    @Override
    public void linked(ServerSession session, Path source, Path target, boolean symLink, Throwable thrown)
            throws IOException {
    }

    @Override
    public void modifyingAttributes(ServerSession session, Path path, Map<String, ?> attrs)
            throws IOException {
    }

    @Override
    public void modifiedAttributes(ServerSession session, Path path, Map<String, ?> attrs, Throwable thrown)
            throws IOException {
    }
}

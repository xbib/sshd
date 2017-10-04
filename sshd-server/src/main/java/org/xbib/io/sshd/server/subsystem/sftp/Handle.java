package org.xbib.io.sshd.server.subsystem.sftp;

import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.server.session.ServerSession;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public abstract class Handle implements java.nio.channels.Channel {
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Path file;
    private final String handle;

    protected Handle(Path file, String handle) {
        this.file = Objects.requireNonNull(file, "No local file path");
        this.handle = ValidateUtils.checkNotNullAndNotEmpty(handle, "No assigned handle for %s", file);
    }

    protected void signalHandleOpening(SftpSubsystem subsystem) throws IOException {
        SftpEventListener listener = subsystem.getSftpEventListenerProxy();
        ServerSession session = subsystem.getServerSession();
        listener.opening(session, handle, this);
    }

    protected void signalHandleOpen(SftpSubsystem subsystem) throws IOException {
        SftpEventListener listener = subsystem.getSftpEventListenerProxy();
        ServerSession session = subsystem.getServerSession();
        listener.open(session, handle, this);
    }

    public Path getFile() {
        return file;
    }

    public String getFileHandle() {
        return handle;
    }

    @Override
    public boolean isOpen() {
        return !closed.get();
    }

    @Override
    public void close() throws IOException {
        if (!closed.getAndSet(true)) {
            return; // debug breakpoint
        }
    }

    @Override
    public String toString() {
        return Objects.toString(getFile());
    }
}

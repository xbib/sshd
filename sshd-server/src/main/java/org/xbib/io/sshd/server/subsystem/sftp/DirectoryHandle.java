package org.xbib.io.sshd.server.subsystem.sftp;

import org.xbib.io.sshd.server.session.ServerSession;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;

/**
 *
 */
public class DirectoryHandle extends Handle implements Iterator<Path> {

    private boolean done;
    private boolean sendDotDot = true;
    private boolean sendDot = true;
    // the directory should be read once at "open directory"
    private DirectoryStream<Path> ds;
    private Iterator<Path> fileList;

    public DirectoryHandle(SftpSubsystem subsystem, Path dir, String handle) throws IOException {
        super(dir, handle);
        signalHandleOpening(subsystem);

        SftpFileSystemAccessor accessor = subsystem.getFileSystemAccessor();
        ServerSession session = subsystem.getServerSession();
        ds = accessor.openDirectory(session, subsystem, dir, handle);

        Path parent = dir.getParent();
        if (parent == null) {
            sendDotDot = false;  // if no parent then no need to send ".."
        }
        fileList = ds.iterator();

        try {
            signalHandleOpen(subsystem);
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    public boolean isDone() {
        return done;
    }

    public void markDone() {
        this.done = true;
        // allow the garbage collector to do the job
        this.fileList = null;
    }

    public boolean isSendDot() {
        return sendDot;
    }

    public void markDotSent() {
        sendDot = false;
    }

    public boolean isSendDotDot() {
        return sendDotDot;
    }

    public void markDotDotSent() {
        sendDotDot = false;
    }

    @Override
    public boolean hasNext() {
        return fileList.hasNext();
    }

    @Override
    public Path next() {
        return fileList.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not allowed to remove " + toString());
    }

    @Override
    public void close() throws IOException {
        super.close();
        markDone(); // just making sure
        ds.close();
    }
}

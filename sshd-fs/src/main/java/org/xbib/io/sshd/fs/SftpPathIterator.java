package org.xbib.io.sshd.fs;

import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 */
public class SftpPathIterator implements Iterator<Path> {
    private final SftpPath p;
    private final Iterator<? extends SftpClient.DirEntry> it;
    private boolean dotIgnored;
    private boolean dotdotIgnored;
    private SftpClient.DirEntry curEntry;

    public SftpPathIterator(SftpPath path, Iterable<? extends SftpClient.DirEntry> iter) {
        this(path, (iter == null) ? null : iter.iterator());
    }

    public SftpPathIterator(SftpPath path, Iterator<? extends SftpClient.DirEntry> iter) {
        p = path;
        it = iter;
        curEntry = nextEntry();
    }

    @Override
    public boolean hasNext() {
        return curEntry != null;
    }

    @Override
    public Path next() {
        if (curEntry == null) {
            throw new NoSuchElementException("No next entry");
        }

        SftpClient.DirEntry entry = curEntry;
        curEntry = nextEntry();
        return p.resolve(entry.getFilename());
    }

    private SftpClient.DirEntry nextEntry() {
        while ((it != null) && it.hasNext()) {
            SftpClient.DirEntry entry = it.next();
            String name = entry.getFilename();
            if (".".equals(name) && (!dotIgnored)) {
                dotIgnored = true;
            } else if ("..".equals(name) && (!dotdotIgnored)) {
                dotdotIgnored = true;
            } else {
                return entry;
            }
        }

        return null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("newDirectoryStream(" + p + ") Iterator#remove() N/A");
    }
}

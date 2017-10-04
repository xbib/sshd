package org.xbib.io.sshd.common.scp;

import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * A no-op implementation of {@link ScpTransferEventListener} for those who wish to
 * implement only a small number of methods. By default, all non-overridden methods
 * simply log at TRACE level their invocation parameters.
 */
public abstract class AbstractScpTransferEventListenerAdapter
        extends AbstractLoggingBean
        implements ScpTransferEventListener {
    protected AbstractScpTransferEventListenerAdapter() {
        super();
    }

    @Override
    public void startFileEvent(FileOperation op, Path file, long length, Set<PosixFilePermission> perms)
            throws IOException {
    }

    @Override
    public void endFileEvent(FileOperation op, Path file, long length, Set<PosixFilePermission> perms, Throwable thrown)
            throws IOException {
    }

    @Override
    public void startFolderEvent(FileOperation op, Path file, Set<PosixFilePermission> perms) throws IOException {
    }

    @Override
    public void endFolderEvent(FileOperation op, Path file, Set<PosixFilePermission> perms, Throwable thrown)
            throws IOException {
    }
}

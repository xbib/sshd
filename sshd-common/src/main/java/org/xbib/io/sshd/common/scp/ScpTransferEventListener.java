package org.xbib.io.sshd.common.scp;

import org.xbib.io.sshd.common.util.SshdEventListener;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * Can be registered in order to receive events about SCP transfers.
 */
public interface ScpTransferEventListener extends SshdEventListener {
    /**
     * An &quot;empty&quot; implementation to be used instead of {@code null}s
     */
    ScpTransferEventListener EMPTY = new ScpTransferEventListener() {
        @Override
        public String toString() {
            return "EMPTY";
        }
    };

    static <L extends ScpTransferEventListener> L validateListener(L listener) {
        return SshdEventListener.validateListener(listener, ScpTransferEventListener.class.getSimpleName());
    }

    /**
     * @param op     The {@link FileOperation}
     * @param file   The <U>local</U> referenced file {@link Path}
     * @param length Size (in bytes) of transferred data
     * @param perms  A {@link Set} of {@link PosixFilePermission}s to be applied
     *               once transfer is complete
     * @throws IOException If failed to handle the event
     */
    default void startFileEvent(FileOperation op, Path file, long length, Set<PosixFilePermission> perms) throws IOException {
        // ignored
    }

    /**
     * @param op     The {@link FileOperation}
     * @param file   The <U>local</U> referenced file {@link Path}
     * @param length Size (in bytes) of transferred data
     * @param perms  A {@link Set} of {@link PosixFilePermission}s to be applied
     *               once transfer is complete
     * @param thrown The result of the operation attempt - if {@code null} then
     *               reception was successful
     * @throws IOException If failed to handle the event
     */
    default void endFileEvent(FileOperation op, Path file, long length, Set<PosixFilePermission> perms, Throwable thrown)
            throws IOException {
        // ignored
    }

    /**
     * @param op    The {@link FileOperation}
     * @param file  The <U>local</U> referenced folder {@link Path}
     * @param perms A {@link Set} of {@link PosixFilePermission}s to be applied
     *              once transfer is complete
     * @throws IOException If failed to handle the event
     */
    default void startFolderEvent(FileOperation op, Path file, Set<PosixFilePermission> perms) throws IOException {
        // ignored
    }

    /**
     * @param op     The {@link FileOperation}
     * @param file   The <U>local</U> referenced file {@link Path}
     * @param perms  A {@link Set} of {@link PosixFilePermission}s to be applied
     *               once transfer is complete
     * @param thrown The result of the operation attempt - if {@code null} then
     *               reception was successful
     * @throws IOException If failed to handle the event
     */
    default void endFolderEvent(FileOperation op, Path file, Set<PosixFilePermission> perms, Throwable thrown)
            throws IOException {
        // ignored
    }

    enum FileOperation {
        SEND,
        RECEIVE
    }
}

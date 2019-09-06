package org.xbib.io.sshd.server.subsystem.sftp;

import org.xbib.io.sshd.server.session.ServerSession;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Set;

/**
 *
 */
public interface SftpFileSystemAccessor {
    SftpFileSystemAccessor DEFAULT = new SftpFileSystemAccessor() {
        @Override
        public String toString() {
            return SftpFileSystemAccessor.class.getSimpleName() + "[DEFAULT]";
        }
    };

    /**
     * Called whenever a new file is opened
     *
     * @param session   The {@link ServerSession} through which the request was received
     * @param subsystem The SFTP subsystem instance that manages the session
     * @param file      The requested <U>local</U> file {@link Path}
     * @param handle    The assigned file handle through which the remote peer references this file.
     *                  May be {@code null}/empty if the request is due to some internal functionality
     *                  instead of due to peer requesting a handle to a file.
     * @param options   The requested {@link OpenOption}s
     * @param attrs     The requested {@link FileAttribute}s
     * @return The opened {@link SeekableByteChannel}
     * @throws IOException If failed to open
     */
    default SeekableByteChannel openFile(
            ServerSession session, SftpEventListenerManager subsystem,
            Path file, String handle, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
            throws IOException {
        return FileChannel.open(file, options, attrs);
    }

    /**
     * Called when locking a section of a file is requested
     *
     * @param session   The {@link ServerSession} through which the request was received
     * @param subsystem The SFTP subsystem instance that manages the session
     * @param file      The requested <U>local</U> file {@link Path}
     * @param handle    The assigned file handle through which the remote peer references this file
     * @param channel   The original {@link Channel} that was returned by {@link #openFile(ServerSession, SftpEventListenerManager, Path, String, Set, FileAttribute...)}
     * @param position  The position at which the locked region is to start - must be non-negative
     * @param size      The size of the locked region; must be non-negative, and the sum
     *                  {@code position}&nbsp;+&nbsp;{@code size} must be non-negative
     * @param shared    {@code true} to request a shared lock, {@code false} to request an exclusive lock
     * @return A lock object representing the newly-acquired lock, or {@code null}
     * if the lock could not be acquired because another program holds an overlapping lock
     * @throws IOException If failed to honor the request
     * @see FileChannel#tryLock(long, long, boolean)
     */
    default FileLock tryLock(ServerSession session, SftpEventListenerManager subsystem,
                             Path file, String handle, Channel channel, long position, long size, boolean shared)
            throws IOException {
        if (!(channel instanceof FileChannel)) {
            throw new StreamCorruptedException("Non file channel to lock: " + channel);
        }

        return ((FileChannel) channel).lock(position, size, shared);
    }

    /**
     * Called when file meta-data re-synchronization is required
     *
     * @param session   The {@link ServerSession} through which the request was received
     * @param subsystem The SFTP subsystem instance that manages the session
     * @param file      The requested <U>local</U> file {@link Path}
     * @param handle    The assigned file handle through which the remote peer references this file
     * @param channel   The original {@link Channel} that was returned by {@link #openFile(ServerSession, SftpEventListenerManager, Path, String, Set, FileAttribute...)}
     * @throws IOException If failed to execute the request
     * @see FileChannel#force(boolean)
     * @see <A HREF="https://github.com/openssh/openssh-portable/blob/master/PROTOCOL">OpenSSH -  section 10</A>
     */
    default void syncFileData(ServerSession session, SftpEventListenerManager subsystem,
                              Path file, String handle, Channel channel)
            throws IOException {
        if (!(channel instanceof FileChannel)) {
            throw new StreamCorruptedException("Non file channel to sync: " + channel);
        }

        ((FileChannel) channel).force(true);
    }

    /**
     * Called when a new directory stream is requested
     *
     * @param session   The {@link ServerSession} through which the request was received
     * @param subsystem The SFTP subsystem instance that manages the session
     * @param dir       The requested <U>local</U> directory
     * @param handle    The assigned directory handle through which the remote peer references this directory
     * @return The opened {@link DirectoryStream}
     * @throws IOException If failed to open
     */
    default DirectoryStream<Path> openDirectory(
            ServerSession session, SftpEventListenerManager subsystem, Path dir, String handle)
            throws IOException {
        return Files.newDirectoryStream(dir);
    }
}

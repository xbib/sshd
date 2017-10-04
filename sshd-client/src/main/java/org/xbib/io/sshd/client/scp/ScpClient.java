package org.xbib.io.sshd.client.scp;

import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.client.session.ClientSessionHolder;
import org.xbib.io.sshd.common.scp.ScpHelper;
import org.xbib.io.sshd.common.scp.ScpTimestamp;
import org.xbib.io.sshd.common.session.SessionHolder;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 */
public interface ScpClient extends SessionHolder<ClientSession>, ClientSessionHolder {
    /**
     * Configurable value of the {@link org.xbib.io.sshd.common.FactoryManager}
     * for controlling the wait timeout for opening a channel for an SCP command
     * in milliseconds. If not specified, then {@link #DEFAULT_EXEC_CHANNEL_OPEN_TIMEOUT}
     * value is used
     */
    String SCP_EXEC_CHANNEL_OPEN_TIMEOUT = "scp-exec-channel-open-timeout";
    long DEFAULT_EXEC_CHANNEL_OPEN_TIMEOUT = TimeUnit.SECONDS.toMillis(30L);
    /**
     * Configurable value of the {@link org.xbib.io.sshd.common.FactoryManager}
     * for controlling the wait timeout for waiting on a channel exit status'
     * for an SCP command in milliseconds. If not specified, then
     * {@link #DEFAULT_EXEC_CHANNEL_EXIT_STATUS_TIMEOUT}
     * value is used. If non-positive, then no wait is performed and the command
     * is assumed to have completed successfully.
     */
    String SCP_EXEC_CHANNEL_EXIT_STATUS_TIMEOUT = "scp-exec-channel-exit-status-timeout";
    long DEFAULT_EXEC_CHANNEL_EXIT_STATUS_TIMEOUT = TimeUnit.SECONDS.toMillis(5L);

    static String createSendCommand(String remote, Collection<Option> options) {
        StringBuilder sb = new StringBuilder(remote.length() + Long.SIZE).append(ScpHelper.SCP_COMMAND_PREFIX);
        if (options.contains(Option.Recursive)) {
            sb.append(" -r");
        }
        if (options.contains(Option.TargetIsDirectory)) {
            sb.append(" -d");
        }
        if (options.contains(Option.PreserveAttributes)) {
            sb.append(" -p");
        }

        sb.append(" -t").append(" --").append(" ").append(remote);
        return sb.toString();
    }

    static String createReceiveCommand(String remote, Collection<Option> options) {
        ValidateUtils.checkNotNullAndNotEmpty(remote, "No remote location specified");
        StringBuilder sb = new StringBuilder(remote.length() + Long.SIZE).append(ScpHelper.SCP_COMMAND_PREFIX);
        if (options.contains(Option.Recursive)) {
            sb.append(" -r");
        }
        if (options.contains(Option.PreserveAttributes)) {
            sb.append(" -p");
        }

        sb.append(" -f").append(" --").append(' ').append(remote);
        return sb.toString();
    }

    default void download(String remote, String local, Option... options) throws IOException {
        download(remote, local, GenericUtils.of(options));
    }

    void download(String remote, String local, Collection<Option> options) throws IOException;

    default void download(String remote, Path local, Option... options) throws IOException {
        download(remote, local, GenericUtils.of(options));
    }

    void download(String remote, Path local, Collection<Option> options) throws IOException;

    // NOTE: the remote location MUST be a file or an exception is generated
    void download(String remote, OutputStream local) throws IOException;

    default byte[] downloadBytes(String remote) throws IOException {
        try (ByteArrayOutputStream local = new ByteArrayOutputStream()) {
            download(remote, local);
            return local.toByteArray();
        }
    }

    default void download(String[] remote, String local, Option... options) throws IOException {
        download(remote, local, GenericUtils.of(options));
    }

    default void download(String[] remote, Path local, Option... options) throws IOException {
        download(remote, local, GenericUtils.of(options));
    }

    void download(String[] remote, String local, Collection<Option> options) throws IOException;

    void download(String[] remote, Path local, Collection<Option> options) throws IOException;

    default void upload(String local, String remote, Option... options) throws IOException {
        upload(local, remote, GenericUtils.of(options));
    }

    default void upload(String local, String remote, Collection<Option> options) throws IOException {
        upload(new String[]{ValidateUtils.checkNotNullAndNotEmpty(local, "Invalid argument local: %s", local)}, remote, options);
    }

    default void upload(Path local, String remote, Option... options) throws IOException {
        upload(local, remote, GenericUtils.of(options));
    }

    default void upload(Path local, String remote, Collection<Option> options) throws IOException {
        upload(new Path[]{ValidateUtils.checkNotNull(local, "Invalid local argument: %s", local)}, remote, GenericUtils.of(options));
    }

    default void upload(String[] local, String remote, Option... options) throws IOException {
        upload(local, remote, GenericUtils.of(options));
    }

    void upload(String[] local, String remote, Collection<Option> options) throws IOException;

    default void upload(Path[] local, String remote, Option... options) throws IOException {
        upload(local, remote, GenericUtils.of(options));
    }

    void upload(Path[] local, String remote, Collection<Option> options) throws IOException;

    // NOTE: due to SCP command limitations, the amount of data to be uploaded must be known a-priori
    // To upload a dynamic amount of data use SFTP
    default void upload(byte[] data, String remote, Collection<PosixFilePermission> perms, ScpTimestamp time) throws IOException {
        upload(data, 0, data.length, remote, perms, time);
    }

    default void upload(byte[] data, int offset, int len, String remote, Collection<PosixFilePermission> perms, ScpTimestamp time) throws IOException {
        try (InputStream local = new ByteArrayInputStream(data, offset, len)) {
            upload(local, remote, len, perms, time);
        }
    }

    void upload(InputStream local, String remote, long size, Collection<PosixFilePermission> perms, ScpTimestamp time) throws IOException;

    enum Option {
        Recursive,
        PreserveAttributes,
        TargetIsDirectory
    }
}

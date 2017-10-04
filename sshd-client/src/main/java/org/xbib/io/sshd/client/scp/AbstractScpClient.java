package org.xbib.io.sshd.client.scp;

import org.xbib.io.sshd.client.channel.ChannelExec;
import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.channel.ClientChannel;
import org.xbib.io.sshd.common.channel.ClientChannelEvent;
import org.xbib.io.sshd.common.file.FileSystemFactory;
import org.xbib.io.sshd.common.scp.ScpException;
import org.xbib.io.sshd.common.scp.ScpHelper;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.io.IoUtils;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 *
 */
public abstract class AbstractScpClient extends AbstractLoggingBean implements ScpClient {
    public static final Set<ClientChannelEvent> COMMAND_WAIT_EVENTS =
            Collections.unmodifiableSet(EnumSet.of(ClientChannelEvent.EXIT_STATUS, ClientChannelEvent.CLOSED));

    protected AbstractScpClient() {
        super();
    }

    @Override
    public final ClientSession getSession() {
        return getClientSession();
    }

    @Override
    public void download(String[] remote, String local, Collection<Option> options) throws IOException {
        local = ValidateUtils.checkNotNullAndNotEmpty(local, "Invalid argument local: %s", local);
        remote = ValidateUtils.checkNotNullAndNotEmpty(remote, "Invalid argument remote: %s", (Object) remote);

        if (remote.length > 1) {
            options = addTargetIsDirectory(options);
        }

        for (String r : remote) {
            download(r, local, options);
        }
    }

    @Override
    public void download(String[] remote, Path local, Collection<Option> options) throws IOException {
        remote = ValidateUtils.checkNotNullAndNotEmpty(remote, "Invalid argument remote: %s", (Object) remote);

        if (remote.length > 1) {
            options = addTargetIsDirectory(options);
        }

        for (String r : remote) {
            download(r, local, options);
        }
    }

    @Override
    public void download(String remote, Path local, Collection<Option> options) throws IOException {
        local = ValidateUtils.checkNotNull(local, "Invalid argument local: %s", local);
        remote = ValidateUtils.checkNotNullAndNotEmpty(remote, "Invalid argument remote: %s", remote);

        LinkOption[] opts = IoUtils.getLinkOptions(true);
        if (Files.isDirectory(local, opts)) {
            options = addTargetIsDirectory(options);
        }

        if (options.contains(Option.TargetIsDirectory)) {
            Boolean status = IoUtils.checkFileExists(local, opts);
            if (status == null) {
                throw new SshException("Target directory " + local.toString() + " is probably inaccesible");
            }

            if (!status) {
                throw new SshException("Target directory " + local.toString() + " does not exist");
            }

            if (!Files.isDirectory(local, opts)) {
                throw new SshException("Target directory " + local.toString() + " is not a directory");
            }
        }

        download(remote, local.getFileSystem(), local, options);
    }

    @Override
    public void download(String remote, String local, Collection<Option> options) throws IOException {
        local = ValidateUtils.checkNotNullAndNotEmpty(local, "Invalid argument local: %s", local);

        ClientSession session = getClientSession();
        FactoryManager manager = session.getFactoryManager();
        FileSystemFactory factory = manager.getFileSystemFactory();
        FileSystem fs = factory.createFileSystem(session);
        try {
            download(remote, fs, fs.getPath(local), options);
        } finally {
            try {
                fs.close();
            } catch (UnsupportedOperationException e) {
            }
        }
    }

    protected abstract void download(String remote, FileSystem fs, Path local, Collection<Option> options) throws IOException;

    @Override
    public void upload(String[] local, String remote, Collection<Option> options) throws IOException {
        final Collection<String> paths = Arrays.asList(ValidateUtils.checkNotNullAndNotEmpty(local, "Invalid argument local: %s", (Object) local));
        runUpload(remote, options, paths, (helper, local1, sendOptions) ->
                helper.send(local1,
                        sendOptions.contains(Option.Recursive),
                        sendOptions.contains(Option.PreserveAttributes),
                        ScpHelper.DEFAULT_SEND_BUFFER_SIZE));
    }

    @Override
    public void upload(Path[] local, String remote, Collection<Option> options) throws IOException {
        final Collection<Path> paths = Arrays.asList(ValidateUtils.checkNotNullAndNotEmpty(local, "Invalid argument local: %s", (Object) local));
        runUpload(remote, options, paths, (helper, local1, sendOptions) ->
                helper.sendPaths(local1,
                        sendOptions.contains(Option.Recursive),
                        sendOptions.contains(Option.PreserveAttributes),
                        ScpHelper.DEFAULT_SEND_BUFFER_SIZE));
    }

    protected abstract <T> void runUpload(String remote, Collection<Option> options, Collection<T> local, ScpOperationExecutor<T> executor) throws IOException;

    /**
     * Invoked by the various <code>upload/download</code> methods after having successfully
     * completed the remote copy command and (optionally) having received an exit status
     * from the remote server. If no exit status received within {@link FactoryManager#CHANNEL_CLOSE_TIMEOUT}
     * the no further action is taken. Otherwise, the exit status is examined to ensure it
     * is either OK or WARNING - if not, an {@link ScpException} is thrown
     *
     * @param cmd     The attempted remote copy command
     * @param channel The {@link ClientChannel} through which the command was sent - <B>Note:</B>
     *                then channel may be in the process of being closed
     * @throws IOException If failed the command
     * @see #handleCommandExitStatus(String, Integer)
     */
    protected void handleCommandExitStatus(String cmd, ClientChannel channel) throws IOException {
        // give a chance for the exit status to be received
        long timeout = channel.getLongProperty(SCP_EXEC_CHANNEL_EXIT_STATUS_TIMEOUT, DEFAULT_EXEC_CHANNEL_EXIT_STATUS_TIMEOUT);
        if (timeout <= 0L) {
            handleCommandExitStatus(cmd, (Integer) null);
            return;
        }

        long waitStart = System.nanoTime();
        Collection<ClientChannelEvent> events = channel.waitFor(COMMAND_WAIT_EVENTS, timeout);
        long waitEnd = System.nanoTime();

        /*
         * There are sometimes race conditions in the order in which channels are closed and exit-status
         * sent by the remote peer (if at all), thus there is no guarantee that we will have an exit
         * status here
         */
        handleCommandExitStatus(cmd, channel.getExitStatus());
    }

    /**
     * Invoked by the various <code>upload/download</code> methods after having successfully
     * completed the remote copy command and (optionally) having received an exit status
     * from the remote server
     *
     * @param cmd        The attempted remote copy command
     * @param exitStatus The exit status - if {@code null} then no status was reported
     * @throws IOException If failed the command
     */
    protected void handleCommandExitStatus(String cmd, Integer exitStatus) throws IOException {

        if (exitStatus == null) {
            return;
        }

        int statusCode = exitStatus;
        switch (statusCode) {
            case ScpHelper.OK:  // do nothing
                break;
            case ScpHelper.WARNING:
                break;
            default:
                throw new ScpException("Failed to run command='" + cmd + "': " + ScpHelper.getExitStatusName(exitStatus), exitStatus);
        }
    }

    protected Collection<Option> addTargetIsDirectory(Collection<Option> options) {
        if (GenericUtils.isEmpty(options) || (!options.contains(Option.TargetIsDirectory))) {
            // create a copy in case the original collection is un-modifiable
            options = GenericUtils.isEmpty(options) ? EnumSet.noneOf(Option.class) : GenericUtils.of(options);
            options.add(Option.TargetIsDirectory);
        }

        return options;
    }

    protected ChannelExec openCommandChannel(ClientSession session, String cmd) throws IOException {
        long waitTimeout = session.getLongProperty(SCP_EXEC_CHANNEL_OPEN_TIMEOUT, DEFAULT_EXEC_CHANNEL_OPEN_TIMEOUT);
        ChannelExec channel = session.createExecChannel(cmd);

        long startTime = System.nanoTime();
        try {
            channel.open().verify(waitTimeout);
            long endTime = System.nanoTime();
            long nanosWait = endTime - startTime;
            return channel;
        } catch (IOException | RuntimeException e) {
            long endTime = System.nanoTime();
            long nanosWait = endTime - startTime;
            channel.close(false);
            throw e;
        }
    }

    @FunctionalInterface
    public interface ScpOperationExecutor<T> {
        void execute(ScpHelper helper, Collection<T> local, Collection<Option> options) throws IOException;
    }
}

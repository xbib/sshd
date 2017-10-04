package org.xbib.io.sshd.server.scp;

import org.xbib.io.sshd.common.file.FileSystemAware;
import org.xbib.io.sshd.common.scp.ScpException;
import org.xbib.io.sshd.common.scp.ScpFileOpener;
import org.xbib.io.sshd.common.scp.ScpHelper;
import org.xbib.io.sshd.common.scp.ScpTransferEventListener;
import org.xbib.io.sshd.common.scp.helpers.DefaultScpFileOpener;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.session.SessionHolder;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;
import org.xbib.io.sshd.common.util.threads.ThreadUtils;
import org.xbib.io.sshd.server.Command;
import org.xbib.io.sshd.server.Environment;
import org.xbib.io.sshd.server.ExitCallback;
import org.xbib.io.sshd.server.SessionAware;
import org.xbib.io.sshd.server.session.ServerSession;
import org.xbib.io.sshd.server.session.ServerSessionHolder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * This commands provide SCP support on both server and client side.
 * Permissions and preservation of access / modification times on files
 * are not supported.
 */
public class ScpCommand
        extends AbstractLoggingBean
        implements Command, Runnable, FileSystemAware, SessionAware,
        SessionHolder<Session>, ServerSessionHolder {

    protected final String name;
    protected final int sendBufferSize;
    protected final int receiveBufferSize;
    protected final ScpFileOpener opener;

    protected boolean optR;
    protected boolean optT;
    protected boolean optF;
    protected boolean optD;
    protected boolean optP; // TODO: handle modification times
    protected FileSystem fileSystem;
    protected String path;
    protected InputStream in;
    protected OutputStream out;
    protected OutputStream err;
    protected ExitCallback callback;
    protected IOException error;
    protected ExecutorService executors;
    protected boolean shutdownExecutor;
    protected Future<?> pendingFuture;
    protected ScpTransferEventListener listener;
    protected ServerSession serverSession;

    /**
     * @param command         The command to be executed
     * @param executorService An {@link ExecutorService} to be used when
     *                        {@link #start(Environment)}-ing execution. If {@code null} an ad-hoc
     *                        single-threaded service is created and used.
     * @param shutdownOnExit  If {@code true} the {@link ExecutorService#shutdownNow()}
     *                        will be called when command terminates - unless it is the ad-hoc
     *                        service, which will be shutdown regardless
     * @param sendSize        Size (in bytes) of buffer to use when sending files
     * @param receiveSize     Size (in bytes) of buffer to use when receiving files
     * @param fileOpener      The {@link ScpFileOpener} - if {@code null} then {@link DefaultScpFileOpener} is used
     * @param eventListener   An {@link ScpTransferEventListener} - may be {@code null}
     * @see ThreadUtils#newSingleThreadExecutor(String)
     * @see ScpHelper#MIN_SEND_BUFFER_SIZE
     * @see ScpHelper#MIN_RECEIVE_BUFFER_SIZE
     */
    public ScpCommand(String command,
                      ExecutorService executorService, boolean shutdownOnExit,
                      int sendSize, int receiveSize,
                      ScpFileOpener fileOpener, ScpTransferEventListener eventListener) {
        name = command;

        if (executorService == null) {
            String poolName = command.replace(' ', '_').replace('/', ':');
            executors = ThreadUtils.newSingleThreadExecutor(poolName);
            shutdownExecutor = true;    // we always close the ad-hoc executor service
        } else {
            executors = executorService;
            shutdownExecutor = shutdownOnExit;
        }

        if (sendSize < ScpHelper.MIN_SEND_BUFFER_SIZE) {
            throw new IllegalArgumentException("<ScpCommmand>(" + command + ") send buffer size "
                    + "(" + sendSize + ") below minimum required "
                    + "(" + ScpHelper.MIN_SEND_BUFFER_SIZE + ")");
        }
        sendBufferSize = sendSize;

        if (receiveSize < ScpHelper.MIN_RECEIVE_BUFFER_SIZE) {
            throw new IllegalArgumentException("<ScpCommmand>(" + command + ") receive buffer size "
                    + "(" + sendSize + ") below minimum required "
                    + "(" + ScpHelper.MIN_RECEIVE_BUFFER_SIZE + ")");
        }
        receiveBufferSize = receiveSize;

        opener = (fileOpener == null) ? DefaultScpFileOpener.INSTANCE : fileOpener;
        listener = (eventListener == null) ? ScpTransferEventListener.EMPTY : eventListener;

        String[] args = GenericUtils.split(command, ' ');
        int numArgs = GenericUtils.length(args);
        for (int i = 1; i < numArgs; i++) {
            String argVal = args[i];
            if (argVal.charAt(0) == '-') {
                for (int j = 1; j < argVal.length(); j++) {
                    char option = argVal.charAt(j);
                    switch (option) {
                        case 'f':
                            optF = true;
                            break;
                        case 'p':
                            optP = true;
                            break;
                        case 'r':
                            optR = true;
                            break;
                        case 't':
                            optT = true;
                            break;
                        case 'd':
                            optD = true;
                            break;
                        default:  // ignored
                            break;
                    }
                }
            } else {
                String prevArg = args[i - 1];
                path = command.substring(command.indexOf(prevArg) + prevArg.length() + 1);

                int pathLen = path.length();
                char startDelim = path.charAt(0);
                char endDelim = (pathLen > 2) ? path.charAt(pathLen - 1) : '\0';
                // remove quotes
                if ((pathLen > 2) && (startDelim == endDelim) && ((startDelim == '\'') || (startDelim == '"'))) {
                    path = path.substring(1, pathLen - 1);
                }
                break;
            }
        }
        if (!optF && !optT) {
            error = new IOException("Either -f or -t option should be set for " + command);
        }
    }

    @Override
    public Session getSession() {
        return getServerSession();
    }

    @Override
    public void setSession(ServerSession session) {
        serverSession = session;
    }

    @Override
    public ServerSession getServerSession() {
        return serverSession;
    }

    @Override
    public void setInputStream(InputStream in) {
        this.in = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

    @Override
    public void setFileSystem(FileSystem fs) {
        this.fileSystem = fs;
    }

    @Override
    public void start(Environment env) throws IOException {
        if (error != null) {
            throw error;
        }

        try {
            pendingFuture = executors.submit(this);
        } catch (RuntimeException e) {    // e.g., RejectedExecutionException
            throw new IOException(e);
        }
    }

    @Override
    public void destroy() {
        // if thread has not completed, cancel it
        if ((pendingFuture != null) && (!pendingFuture.isDone())) {
            boolean result = pendingFuture.cancel(true);
            // TODO consider waiting some reasonable (?) amount of time for cancellation
        }

        pendingFuture = null;

        if ((executors != null) && (!executors.isShutdown()) && shutdownExecutor) {
            Collection<Runnable> runners = executors.shutdownNow();
        }

        executors = null;

        try {
            fileSystem.close();
        } catch (UnsupportedOperationException e) {
            // Ignore
        } catch (IOException e) {
        }
    }

    @Override
    public void run() {
        int exitValue = ScpHelper.OK;
        String exitMessage = null;
        ScpHelper helper = new ScpHelper(getServerSession(), in, out, fileSystem, opener, listener);
        try {
            if (optT) {
                helper.receive(helper.resolveLocalPath(path), optR, optD, optP, receiveBufferSize);
            } else if (optF) {
                helper.send(Collections.singletonList(path), optR, optP, sendBufferSize);
            } else {
                throw new IOException("Unsupported mode");
            }
        } catch (IOException e) {
            ServerSession session = getServerSession();
            try {
                Integer statusCode = null;
                if (e instanceof ScpException) {
                    statusCode = ((ScpException) e).getExitStatus();
                }
                exitValue = (statusCode == null) ? ScpHelper.ERROR : statusCode;
                // this is an exception so status cannot be OK/WARNING
                if ((exitValue == ScpHelper.OK) || (exitValue == ScpHelper.WARNING)) {
                    exitValue = ScpHelper.ERROR;
                }
                exitMessage = GenericUtils.trimToEmpty(e.getMessage());
                writeCommandResponseMessage(name, exitValue, exitMessage);
            } catch (IOException e2) {
            }
        } finally {
            if (callback != null) {
                callback.onExit(exitValue, GenericUtils.trimToEmpty(exitMessage));
            }
        }
    }

    protected void writeCommandResponseMessage(String command, int exitValue, String exitMessage) throws IOException {
        ScpHelper.sendResponseMessage(out, exitValue, exitMessage);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getSession() + ") " + name;
    }
}
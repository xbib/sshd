package org.xbib.io.sshd.server.shell;

import org.xbib.io.sshd.common.RuntimeSshException;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.io.IoUtils;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;
import org.xbib.io.sshd.common.util.threads.ThreadUtils;
import org.xbib.io.sshd.server.Command;
import org.xbib.io.sshd.server.Environment;
import org.xbib.io.sshd.server.ExitCallback;
import org.xbib.io.sshd.server.SessionAware;
import org.xbib.io.sshd.server.session.ServerSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * A shell implementation that wraps an instance of {@link org.xbib.io.sshd.server.shell.InvertedShell}
 * as a {@link Command}.  This is useful when using external
 * processes.
 * When starting the shell, this wrapper will also create a thread used
 * to pump the streams and also to check if the shell is alive.
 */
public class InvertedShellWrapper extends AbstractLoggingBean implements Command, SessionAware {

    /**
     * Default buffer size for the I/O pumps.
     */
    public static final int DEFAULT_BUFFER_SIZE = IoUtils.DEFAULT_COPY_SIZE;

    /**
     * Value used to control the &quot;busy-wait&quot; sleep time (millis) on
     * the pumping loop if nothing was pumped - must be <U>positive</U>
     *
     * @see #DEFAULT_PUMP_SLEEP_TIME
     */
    public static final String PUMP_SLEEP_TIME = "inverted-shell-wrapper-pump-sleep";

    /**
     * Default value for {@link #PUMP_SLEEP_TIME} if none set
     */
    public static final long DEFAULT_PUMP_SLEEP_TIME = 1L;

    private final org.xbib.io.sshd.server.shell.InvertedShell shell;
    private final Executor executor;
    private final int bufferSize;
    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private OutputStream shellIn;
    private InputStream shellOut;
    private InputStream shellErr;
    private ExitCallback callback;
    private boolean shutdownExecutor;
    private long pumpSleepTime = DEFAULT_PUMP_SLEEP_TIME;

    /**
     * Auto-allocates an {@link Executor} in order to create the streams pump thread
     * and uses the {@link #DEFAULT_BUFFER_SIZE}
     *
     * @param shell The {@link org.xbib.io.sshd.server.shell.InvertedShell}
     * @see #InvertedShellWrapper(org.xbib.io.sshd.server.shell.InvertedShell, int)
     */
    public InvertedShellWrapper(org.xbib.io.sshd.server.shell.InvertedShell shell) {
        this(shell, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Auto-allocates an {@link Executor} in order to create the streams pump thread
     *
     * @param shell      The {@link org.xbib.io.sshd.server.shell.InvertedShell}
     * @param bufferSize Buffer size to use - must be above min. size ({@link Byte#SIZE})
     * @see #InvertedShellWrapper(org.xbib.io.sshd.server.shell.InvertedShell, Executor, boolean, int)
     */
    public InvertedShellWrapper(org.xbib.io.sshd.server.shell.InvertedShell shell, int bufferSize) {
        this(shell, null, true, bufferSize);
    }

    /**
     * @param shell            The {@link org.xbib.io.sshd.server.shell.InvertedShell}
     * @param executor         The {@link Executor} to use in order to create the streams pump thread.
     *                         If {@code null} one is auto-allocated and shutdown when wrapper is {@link #destroy()}-ed.
     * @param shutdownExecutor If {@code true} the executor is shut down when shell wrapper is {@link #destroy()}-ed.
     *                         Ignored if executor service auto-allocated
     * @param bufferSize       Buffer size to use - must be above min. size ({@link Byte#SIZE})
     */
    public InvertedShellWrapper(InvertedShell shell, Executor executor, boolean shutdownExecutor, int bufferSize) {
        this.shell = Objects.requireNonNull(shell, "No shell");
        this.executor = (executor == null) ? ThreadUtils.newSingleThreadExecutor("shell[0x" + Integer.toHexString(shell.hashCode()) + "]") : executor;
        ValidateUtils.checkTrue(bufferSize > Byte.SIZE, "Copy buffer size too small: %d", bufferSize);
        this.bufferSize = bufferSize;
        this.shutdownExecutor = (executor == null) || shutdownExecutor;
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
    public void setSession(ServerSession session) {
        pumpSleepTime = session.getLongProperty(PUMP_SLEEP_TIME, DEFAULT_PUMP_SLEEP_TIME);
        ValidateUtils.checkTrue(pumpSleepTime > 0L, "Invalid " + PUMP_SLEEP_TIME + ": %d", pumpSleepTime);
        shell.setSession(session);
    }

    @Override
    public synchronized void start(Environment env) throws IOException {
        // TODO propagate the Environment itself and support signal sending.
        shell.start(env);
        shellIn = shell.getInputStream();
        shellOut = shell.getOutputStream();
        shellErr = shell.getErrorStream();
        executor.execute(this::pumpStreams);
    }

    @Override
    public synchronized void destroy() throws Exception {
        Throwable err = null;
        try {
            shell.destroy();
        } catch (Throwable e) {
            err = GenericUtils.accumulateException(err, e);
        }

        if (shutdownExecutor && (executor instanceof ExecutorService)) {
            try {
                ((ExecutorService) executor).shutdown();
            } catch (Exception e) {
                err = GenericUtils.accumulateException(err, e);
            }
        }

        if (err != null) {
            if (err instanceof Exception) {
                throw (Exception) err;
            } else {
                throw new RuntimeSshException(err);
            }
        }
    }

    protected void pumpStreams() {
        try {
            // Use a single thread to correctly sequence the output and error streams.
            // If any bytes are available from the output stream, send them first, then
            // check the error stream, or wait until more data is available.
            for (byte[] buffer = new byte[bufferSize]; ; ) {
                if (pumpStream(in, shellIn, buffer)) {
                    continue;
                }
                if (pumpStream(shellOut, out, buffer)) {
                    continue;
                }
                if (pumpStream(shellErr, err, buffer)) {
                    continue;
                }

                /*
                 * Make sure we exhausted all data - the shell might be dead
                 * but some data may still be in transit via pumping
                 */
                if ((!shell.isAlive()) && (in.available() <= 0) && (shellOut.available() <= 0) && (shellErr.available() <= 0)) {
                    callback.onExit(shell.exitValue());
                    return;
                }

                // Sleep a bit.  This is not very good, as it consumes CPU, but the
                // input streams are not selectable for nio, and any other blocking
                // method would consume at least two threads
                Thread.sleep(pumpSleepTime);
            }
        } catch (Throwable e) {
            try {
                shell.destroy();
            } catch (Throwable err) {
            }

            int exitValue = shell.exitValue();
            callback.onExit(exitValue, e.getClass().getSimpleName());
        }
    }

    protected boolean pumpStream(InputStream in, OutputStream out, byte[] buffer) throws IOException {
        int available = in.available();
        if (available > 0) {
            int len = in.read(buffer);
            if (len > 0) {
                out.write(buffer, 0, len);
                out.flush();
                return true;
            }
        } else if (available == -1) {
            out.close();
        }
        return false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + String.valueOf(shell);
    }
}

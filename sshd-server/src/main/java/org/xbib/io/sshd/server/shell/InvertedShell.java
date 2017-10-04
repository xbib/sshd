package org.xbib.io.sshd.server.shell;

import org.xbib.io.sshd.server.CommandLifecycle;
import org.xbib.io.sshd.server.SessionAware;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * This shell have inverted streams, such as the one obtained when launching a
 * new {@link Process} from java.  This interface is meant to be used with
 * {@link InvertedShellWrapper} class as an implementation of
 * {@link org.xbib.io.sshd.common.Factory}.
 */
public interface InvertedShell extends CommandLifecycle, SessionAware {
    /**
     * Returns the output stream used to feed the shell.
     * This method is called after the shell has been started.
     *
     * @return The {@link OutputStream} used to feed the shell
     */
    OutputStream getInputStream();

    /**
     * @return The {@link InputStream} representing the output stream of the shell
     */
    InputStream getOutputStream();

    /**
     * @return The {@link InputStream} representing the error stream of the shell
     */
    InputStream getErrorStream();

    /**
     * Check if the underlying shell is still alive
     *
     * @return {@code true} if alive
     */
    boolean isAlive();

    /**
     * Retrieve the exit value of the shell.
     * This method must only be called when the shell is not alive anymore.
     *
     * @return the exit value of the shell
     */
    int exitValue();
}

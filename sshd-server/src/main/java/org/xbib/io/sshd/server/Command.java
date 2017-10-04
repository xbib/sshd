package org.xbib.io.sshd.server;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Represents a command, shell or subsystem that can be used to send command.
 * This command have direct streams, meaning those streams will be provided by the ssh server
 * for the shell to use directly. This interface is suitable for implementing commands in java,
 * rather than using external processes.
 */
public interface Command extends CommandLifecycle {

    /**
     * Set the input stream that can be used by the shell to read input.
     *
     * @param in The {@link InputStream}  used by the shell to read input.
     */
    void setInputStream(InputStream in);

    /**
     * Set the output stream that can be used by the shell to write its output.
     *
     * @param out The {@link OutputStream} used by the shell to write its output
     */
    void setOutputStream(OutputStream out);

    /**
     * Set the error stream that can be used by the shell to write its errors.
     *
     * @param err The {@link OutputStream} used by the shell to write its errors
     */
    void setErrorStream(OutputStream err);

    /**
     * Set the callback that the shell has to call when it is closed.
     *
     * @param callback The {@link ExitCallback} to call when shell is closed
     */
    void setExitCallback(ExitCallback callback);
}

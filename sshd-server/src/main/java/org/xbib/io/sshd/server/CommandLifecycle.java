package org.xbib.io.sshd.server;

import java.io.IOException;

/**
 */
public interface CommandLifecycle {
    /**
     * Starts the command execution. All streams must have been set <U>before</U>
     * calling this method. The command should implement {@link Runnable},
     * and this method should spawn a new thread like:
     * <pre>
     * {@code Thread(this).start(); }
     * </pre>
     *
     * @param env The {@link org.xbib.io.sshd.server.Environment}
     * @throws IOException If failed to start
     */
    void start(Environment env) throws IOException;

    /**
     * This method is called by the SSH server to destroy the command because
     * the client has disconnected somehow.
     *
     * @throws Exception if failed to destroy
     */
    void destroy() throws Exception;
}

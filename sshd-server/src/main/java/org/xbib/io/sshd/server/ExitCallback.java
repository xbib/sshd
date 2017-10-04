package org.xbib.io.sshd.server;

/**
 * Callback used by the shell to notify the SSH server is has exited.
 */
@FunctionalInterface
public interface ExitCallback {

    /**
     * Informs the SSH server that the shell has exited.
     *
     * @param exitValue the exit value
     */
    default void onExit(int exitValue) {
        onExit(exitValue, "");
    }

    /**
     * Informs the SSH client/server that the shell has exited.
     *
     * @param exitValue   the exit value
     * @param exitMessage exit value description
     */
    void onExit(int exitValue, String exitMessage);
}

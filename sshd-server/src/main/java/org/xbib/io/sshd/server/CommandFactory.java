package org.xbib.io.sshd.server;

/**
 * A factory of commands.
 * Commands are executed on the server side when an "exec" channel is
 * requested by the SSH client.
 */
@FunctionalInterface
public interface CommandFactory {

    /**
     * Create a command with the given name.
     * If the command is not known, a dummy command should be returned to allow
     * the display output to be sent back to the client.
     *
     * @param command The command that will be run
     * @return a non {@code null} {@link org.xbib.io.sshd.server.Command} instance
     */
    Command createCommand(String command);
}

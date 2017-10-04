package org.xbib.io.sshd.server.scp;

import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.server.Command;
import org.xbib.io.sshd.server.Environment;
import org.xbib.io.sshd.server.ExitCallback;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Implementation of an unknown command that can be returned by <code>CommandFactory</code>
 * when the command is not known, as it is supposed to always
 * return a valid <code>Command</code> object.
 */
public class UnknownCommand implements Command, Runnable {

    private final String command;
    private final String message;
    @SuppressWarnings("unused")
    private InputStream in;
    @SuppressWarnings("unused")
    private OutputStream out;
    private OutputStream err;
    private ExitCallback callback;

    public UnknownCommand(String command) {
        this.command = ValidateUtils.checkNotNullAndNotEmpty(command, "No command");
        this.message = "Unknown command: " + command;
    }

    public String getCommand() {
        return command;
    }

    public String getMessage() {
        return message;
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
    public void run() {
        String errorMessage = getMessage();
        try {
            try {
                err.write(errorMessage.getBytes(StandardCharsets.UTF_8));
                err.write('\n');
            } finally {
                err.flush();
            }
        } catch (IOException e) {
            // ignored
        }

        if (callback != null) {
            callback.onExit(1, errorMessage);
        }
    }

    @Override
    public void start(Environment env) throws IOException {
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void destroy() {
        // ignored
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getCommand());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        return Objects.equals(this.getCommand(), ((UnknownCommand) obj).getCommand());
    }

    @Override
    public String toString() {
        return getMessage();
    }
}

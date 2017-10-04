package org.xbib.io.sshd.server;

import org.xbib.io.sshd.common.io.IoInputStream;
import org.xbib.io.sshd.common.io.IoOutputStream;

/**
 * Represents a command capable of doing non-blocking io.
 * If this interface is implemented by a command, the usual
 * blocking input / output / error streams won't be set.
 */
public interface AsyncCommand extends Command {

    /**
     * Set the input stream that can be used by the shell to read input.
     *
     * @param in The {@link IoInputStream} used by the shell to read input
     */
    void setIoInputStream(IoInputStream in);

    /**
     * Set the output stream that can be used by the shell to write its output.
     *
     * @param out The {@link IoOutputStream} used by the shell to write its output
     */
    void setIoOutputStream(IoOutputStream out);

    /**
     * Set the error stream that can be used by the shell to write its errors.
     *
     * @param err The {@link IoOutputStream} used by the shell to write its errors
     */
    void setIoErrorStream(IoOutputStream err);

}

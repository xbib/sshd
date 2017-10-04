package org.xbib.io.sshd.common.channel;

import org.xbib.io.sshd.common.SshException;

/**
 * Indicates a {@link Window} has been closed.
 */
public class WindowClosedException extends SshException {
    private static final long serialVersionUID = -5345787686165334234L;

    public WindowClosedException(String name) {
        super("Already closed: " + name);
    }
}

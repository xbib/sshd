package org.xbib.io.sshd.common;

/**
 * Exception used in the SSH client or server.
 */
public class RuntimeSshException extends RuntimeException {

    private static final long serialVersionUID = -2423550196146939503L;

    public RuntimeSshException() {
        this(null, null);
    }

    public RuntimeSshException(String message) {
        this(message, null);
    }

    public RuntimeSshException(Throwable cause) {
        this(null, cause);
    }

    public RuntimeSshException(String message, Throwable cause) {
        super(message);
        if (cause != null) {
            initCause(cause);
        }
    }
}

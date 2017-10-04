package org.xbib.io.sshd.common.io;

/**
 *
 */
public class ReadPendingException extends IllegalStateException {
    private static final long serialVersionUID = -3407225601154249841L;

    public ReadPendingException() {
        super();
    }

    public ReadPendingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReadPendingException(String s) {
        super(s);
    }

    public ReadPendingException(Throwable cause) {
        super(cause);
    }
}

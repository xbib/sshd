package org.xbib.io.sshd.common.io;

/**
 *
 */
public class WritePendingException extends IllegalStateException {
    private static final long serialVersionUID = 8814014909076826576L;

    public WritePendingException() {
        super();
    }

    public WritePendingException(String message, Throwable cause) {
        super(message, cause);
    }

    public WritePendingException(String s) {
        super(s);
    }

    public WritePendingException(Throwable cause) {
        super(cause);
    }
}

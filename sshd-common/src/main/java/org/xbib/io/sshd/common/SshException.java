package org.xbib.io.sshd.common;

import org.xbib.io.sshd.common.util.GenericUtils;

import java.io.IOException;
import java.util.Objects;

/**
 * Represents an SSH related exception.
 */
public class SshException extends IOException {

    private static final long serialVersionUID = -7349477687125144606L;

    private final int disconnectCode;

    public SshException(String message) {
        this(message, null);
    }

    public SshException(Throwable cause) {
        this(Objects.requireNonNull(cause, "No cause").getMessage(), cause);
    }

    public SshException(String message, Throwable cause) {
        this(0, message, cause);
    }

    public SshException(int disconnectCode) {
        this(disconnectCode, org.xbib.io.sshd.common.SshConstants.getDisconnectReasonName(disconnectCode));
    }

    public SshException(int disconnectCode, String message) {
        this(disconnectCode, message, null);
    }

    public SshException(int disconnectCode, Throwable cause) {
        this(disconnectCode, org.xbib.io.sshd.common.SshConstants.getDisconnectReasonName(disconnectCode), cause);
    }

    public SshException(int disconnectCode, String message, Throwable cause) {
        super(GenericUtils.isEmpty(message) ? SshConstants.getDisconnectReasonName(disconnectCode) : message);
        this.disconnectCode = disconnectCode;
        if (cause != null) {
            initCause(cause);
        }
    }

    public int getDisconnectCode() {
        return disconnectCode;
    }
}

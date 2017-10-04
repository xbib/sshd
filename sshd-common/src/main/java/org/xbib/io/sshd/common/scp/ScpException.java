package org.xbib.io.sshd.common.scp;

import java.io.IOException;
import java.util.Objects;

/**
 */
public class ScpException extends IOException {
    private static final long serialVersionUID = 7734851624372451732L;
    private final Integer exitStatus;

    public ScpException(String message) {
        this(message, null);
    }

    public ScpException(Integer exitStatus) {
        this("Exit status=" + ScpHelper.getExitStatusName(Objects.requireNonNull(exitStatus, "No exit status")), exitStatus);
    }

    public ScpException(String message, Integer exitStatus) {
        this(message, null, exitStatus);
    }

    public ScpException(Throwable cause, Integer exitStatus) {
        this(Objects.requireNonNull(cause, "No cause").getMessage(), cause, exitStatus);
    }

    public ScpException(String message, Throwable cause, Integer exitStatus) {
        super(message, cause);
        this.exitStatus = exitStatus;
    }

    public Integer getExitStatus() {
        return exitStatus;
    }
}

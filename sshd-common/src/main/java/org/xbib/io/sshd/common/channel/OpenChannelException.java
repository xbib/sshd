package org.xbib.io.sshd.common.channel;

/**
 * Documents failure of a channel to open as expected.
 */
public class OpenChannelException extends Exception {
    private static final long serialVersionUID = 3861183351970782341L;
    private final int code;

    public OpenChannelException(int code, String message) {
        this(code, message, null);
    }

    public OpenChannelException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * The reason code as specified by RFC 4254.
     * <ul>
     * <li>{@link org.xbib.io.sshd.common.SshConstants#SSH_OPEN_ADMINISTRATIVELY_PROHIBITED}
     * <li>{@link org.xbib.io.sshd.common.SshConstants#SSH_OPEN_CONNECT_FAILED}
     * <li>{@link org.xbib.io.sshd.common.SshConstants#SSH_OPEN_UNKNOWN_CHANNEL_TYPE}
     * <li>{@link org.xbib.io.sshd.common.SshConstants#SSH_OPEN_RESOURCE_SHORTAGE}
     * </ul>
     *
     * @return reason code; 0 if no standardized reason code is given.
     */
    public int getReasonCode() {
        return code;
    }
}

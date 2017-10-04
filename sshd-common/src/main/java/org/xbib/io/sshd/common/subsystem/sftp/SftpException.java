package org.xbib.io.sshd.common.subsystem.sftp;

import java.io.IOException;

/**
 *
 */
public class SftpException extends IOException {
    private static final long serialVersionUID = 8096963562429466995L;
    private final int status;

    public SftpException(int status, String msg) {
        super(msg);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "SFTP error (" + SftpConstants.getStatusName(getStatus()) + "): " + getMessage();
    }
}

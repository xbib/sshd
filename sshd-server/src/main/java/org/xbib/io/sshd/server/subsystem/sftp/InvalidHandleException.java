package org.xbib.io.sshd.server.subsystem.sftp;

import java.io.IOException;

/**
 *
 */
public class InvalidHandleException extends IOException {
    private static final long serialVersionUID = -1686077114375131889L;

    public InvalidHandleException(String handle, Handle h, Class<? extends Handle> expected) {
        super(handle + "[" + h + "] is not a " + expected.getSimpleName());
    }
}

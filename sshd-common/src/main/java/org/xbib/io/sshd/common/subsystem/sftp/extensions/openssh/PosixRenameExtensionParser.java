package org.xbib.io.sshd.common.subsystem.sftp.extensions.openssh;

/**
 *
 */
public class PosixRenameExtensionParser extends AbstractOpenSSHExtensionParser {
    public static final String NAME = "posix-rename@openssh.com";
    public static final PosixRenameExtensionParser INSTANCE = new PosixRenameExtensionParser();

    public PosixRenameExtensionParser() {
        super(NAME);
    }
}

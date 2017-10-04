package org.xbib.io.sshd.common.subsystem.sftp.extensions.openssh;

/**
 *
 */
public class FstatVfsExtensionParser extends AbstractOpenSSHExtensionParser {
    public static final String NAME = "fstatvfs@openssh.com";
    public static final FstatVfsExtensionParser INSTANCE = new FstatVfsExtensionParser();

    public FstatVfsExtensionParser() {
        super(NAME);
    }
}

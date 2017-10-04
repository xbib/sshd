package org.xbib.io.sshd.common.subsystem.sftp.extensions.openssh;

/**
 *
 */
public class StatVfsExtensionParser extends AbstractOpenSSHExtensionParser {
    public static final String NAME = "statvfs@openssh.com";
    public static final StatVfsExtensionParser INSTANCE = new StatVfsExtensionParser();

    public StatVfsExtensionParser() {
        super(NAME);
    }
}

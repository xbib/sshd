package org.xbib.io.sshd.common.subsystem.sftp.extensions.openssh;

/**
 *
 */
public class FsyncExtensionParser extends AbstractOpenSSHExtensionParser {
    public static final String NAME = "fsync@openssh.com";
    public static final FsyncExtensionParser INSTANCE = new FsyncExtensionParser();

    public FsyncExtensionParser() {
        super(NAME);
    }
}

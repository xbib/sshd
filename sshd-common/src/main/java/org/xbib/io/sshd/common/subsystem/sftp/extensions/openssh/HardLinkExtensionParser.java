package org.xbib.io.sshd.common.subsystem.sftp.extensions.openssh;

/**
 *
 */
public class HardLinkExtensionParser extends AbstractOpenSSHExtensionParser {
    public static final String NAME = "hardlink@openssh.com";
    public static final HardLinkExtensionParser INSTANCE = new HardLinkExtensionParser();

    public HardLinkExtensionParser() {
        super(NAME);
    }
}

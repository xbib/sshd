package org.xbib.io.sshd.server.subsystem.sftp;

import org.xbib.io.sshd.common.subsystem.sftp.PrincipalBase;

import java.nio.file.attribute.UserPrincipal;

/**
 *
 */
public class DefaultUserPrincipal extends PrincipalBase implements UserPrincipal {

    public DefaultUserPrincipal(String name) {
        super(name);
    }

}

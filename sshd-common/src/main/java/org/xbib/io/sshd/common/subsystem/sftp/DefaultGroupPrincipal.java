package org.xbib.io.sshd.common.subsystem.sftp;

import java.nio.file.attribute.GroupPrincipal;

/**
 *
 */
public class DefaultGroupPrincipal extends PrincipalBase implements GroupPrincipal {

    public DefaultGroupPrincipal(String name) {
        super(name);
    }

}

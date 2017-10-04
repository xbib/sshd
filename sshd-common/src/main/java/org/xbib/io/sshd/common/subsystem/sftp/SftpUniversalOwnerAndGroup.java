package org.xbib.io.sshd.common.subsystem.sftp;

import org.xbib.io.sshd.common.NamedResource;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Some universal identifiers used in owner and/or group specification strings.
 */
public enum SftpUniversalOwnerAndGroup implements NamedResource {
    Owner,          // The owner of the file.
    Group,          // The group associated with the file.
    Everyone,       // The world.
    Interactive,    // Accessed from an interactive terminal.
    Network,        // Accessed via the network.
    Dialup,         // Accessed as a dialup user to the server.
    Batch,          // Accessed from a batch job.
    Anonymous,      // Accessed without any authentication.
    Authenticated,  // Any authenticated user (opposite of ANONYMOUS).
    Service;        // Access from a system service.

    public static final Set<SftpUniversalOwnerAndGroup> VALUES =
            Collections.unmodifiableSet(EnumSet.allOf(SftpUniversalOwnerAndGroup.class));

    private final String name;

    SftpUniversalOwnerAndGroup() {
        name = name().toUpperCase() + "@";
    }

    public static SftpUniversalOwnerAndGroup fromName(String name) {
        return NamedResource.findByName(name, String.CASE_INSENSITIVE_ORDER, VALUES);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }
}

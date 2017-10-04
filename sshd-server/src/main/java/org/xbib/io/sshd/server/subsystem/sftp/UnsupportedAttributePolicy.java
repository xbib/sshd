package org.xbib.io.sshd.server.subsystem.sftp;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 *
 */
public enum UnsupportedAttributePolicy {
    Ignore,
    Warn,
    ThrowException;

    public static final Set<UnsupportedAttributePolicy> VALUES =
            Collections.unmodifiableSet(EnumSet.allOf(UnsupportedAttributePolicy.class));
}
package org.xbib.io.sshd.common.config;

import org.xbib.io.sshd.common.util.GenericUtils;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 */
public enum AllowTcpForwardingValue {
    ALL,
    NONE,
    LOCAL,
    REMOTE;

    public static final Set<AllowTcpForwardingValue> VALUES =
            Collections.unmodifiableSet(EnumSet.allOf(AllowTcpForwardingValue.class));

    // NOTE: it also interprets "yes" as "all" and "no" as "none"
    public static AllowTcpForwardingValue fromString(String s) {
        if (GenericUtils.isEmpty(s)) {
            return null;
        }

        if ("yes".equalsIgnoreCase(s)) {
            return ALL;
        }

        if ("no".equalsIgnoreCase(s)) {
            return NONE;
        }

        for (AllowTcpForwardingValue v : VALUES) {
            if (s.equalsIgnoreCase(v.name())) {
                return v;
            }
        }

        return null;
    }
}

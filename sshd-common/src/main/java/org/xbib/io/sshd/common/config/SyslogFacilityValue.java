package org.xbib.io.sshd.common.config;

import org.xbib.io.sshd.common.util.GenericUtils;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 *
 */
public enum SyslogFacilityValue {
    DAEMON, USER, AUTH, LOCAL0, LOCAL1, LOCAL2, LOCAL3, LOCAL4, LOCAL5, LOCAL6, LOCAL7;

    public static final Set<SyslogFacilityValue> VALUES =
            Collections.unmodifiableSet(EnumSet.allOf(SyslogFacilityValue.class));

    public static SyslogFacilityValue fromName(String n) {
        if (GenericUtils.isEmpty(n)) {
            return null;
        }

        for (SyslogFacilityValue f : VALUES) {
            if (n.equalsIgnoreCase(f.name())) {
                return f;
            }
        }

        return null;
    }
}

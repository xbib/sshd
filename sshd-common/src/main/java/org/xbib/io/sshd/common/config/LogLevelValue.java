package org.xbib.io.sshd.common.config;

import org.xbib.io.sshd.common.util.GenericUtils;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 */
public enum LogLevelValue {
    /*
     * NOTE(s):
     * 1. DEBUG and DEBUG1 are EQUIVALENT
     * 2. Order is important (!!!)
     */
    QUIET, FATAL, ERROR, INFO, VERBOSE, DEBUG, DEBUG1, DEBUG2, DEBUG3;

    public static final Set<LogLevelValue> VALUES =
            Collections.unmodifiableSet(EnumSet.allOf(LogLevelValue.class));

    public static LogLevelValue fromName(String n) {
        if (GenericUtils.isEmpty(n)) {
            return null;
        }

        for (LogLevelValue l : VALUES) {
            if (n.equalsIgnoreCase(l.name())) {
                return l;
            }
        }

        return null;
    }
}

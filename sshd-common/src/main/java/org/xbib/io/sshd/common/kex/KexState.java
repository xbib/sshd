package org.xbib.io.sshd.common.kex;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Used to track the key-exchange (KEX) protocol progression.
 */
public enum KexState {
    UNKNOWN,
    INIT,
    RUN,
    KEYS,
    DONE;

    public static final Set<KexState> VALUES = Collections.unmodifiableSet(EnumSet.allOf(KexState.class));
}

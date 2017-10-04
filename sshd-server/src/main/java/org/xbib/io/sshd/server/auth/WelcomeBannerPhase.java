package org.xbib.io.sshd.server.auth;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Used to indicate at which authentication phase to send the welcome
 * banner (if any configured).
 */
public enum WelcomeBannerPhase {
    /**
     * Immediately after receiving &quot;ssh-userauth&quot; request
     */
    IMMEDIATE,
    /**
     * On first {@code SSH_MSG_USERAUTH_REQUEST}
     */
    FIRST_REQUEST,
    /**
     * On first {@code SSH_MSG_USERAUTH_XXX} extension command
     */
    FIRST_AUTHCMD,
    /**
     * On first {@code SSH_MSG_USERAUTH_FAILURE}
     */
    FIRST_FAILURE,
    /**
     * After user successfully authenticates
     */
    POST_SUCCESS,
    /**
     * Do not send a welcome banner even if one is configured. <B>Note:</B>
     * this option is useful when a global welcome banner has been configured
     * but we want to disable it for a specific session.
     */
    NEVER;

    public static final Set<WelcomeBannerPhase> VALUES =
            Collections.unmodifiableSet(EnumSet.allOf(WelcomeBannerPhase.class));
}

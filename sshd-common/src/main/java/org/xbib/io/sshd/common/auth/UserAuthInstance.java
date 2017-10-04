package org.xbib.io.sshd.common.auth;

import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.session.Session;

/**
 * Represents an authentication-in-progress tracker for a specific session
 *
 * @param <S> The type of session being tracked by the instance
 */
public interface UserAuthInstance<S extends Session> extends NamedResource {
    /**
     * @return The current session for which the authentication is being
     * tracked. <B>Note:</B> may be {@code null} if the instance has not
     * been initialized yet
     */
    S getSession();
}

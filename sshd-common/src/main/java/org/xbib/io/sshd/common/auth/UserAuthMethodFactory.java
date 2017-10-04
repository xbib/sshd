package org.xbib.io.sshd.common.auth;

import org.xbib.io.sshd.common.NamedFactory;

/**
 * Represents a user authentication method
 *
 * @param <M> The authentication method factory type
 */
public interface UserAuthMethodFactory<M> extends NamedFactory<M> {
    /**
     * Password authentication method name
     */
    String PASSWORD = "password";

    /**
     * Public key authentication method name
     */
    String PUBLIC_KEY = "publickey";

    /**
     * Keyboard interactive authentication method
     */
    String KB_INTERACTIVE = "keyboard-interactive";

    /**
     * Host-based authentication method
     */
    String HOST_BASED = "hostbased";
}

package org.xbib.io.sshd.common.config.keys;

import java.io.IOException;

/**
 */
@FunctionalInterface
public interface FilePasswordProvider {
    /**
     * An &quot;empty&quot; provider that returns {@code null} - i.e., unprotected key file
     */
    FilePasswordProvider EMPTY = resourceKey -> null;

    static FilePasswordProvider of(String password) {
        return r -> password;
    }

    /**
     * @param resourceKey The resource key representing the <U>private</U>
     *                    file
     * @return The password - if {@code null}/empty then no password is required
     * @throws IOException if cannot resolve password
     */
    String getPassword(String resourceKey) throws IOException;
}

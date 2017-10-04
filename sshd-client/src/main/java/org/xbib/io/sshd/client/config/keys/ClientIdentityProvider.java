package org.xbib.io.sshd.client.config.keys;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

/**
 *
 */
@FunctionalInterface
public interface ClientIdentityProvider {
    /**
     * Provides a {@link KeyPair} representing the client identity
     *
     * @return The client identity - may be {@code null} if no currently
     * available identity from this provider. <B>Note:</B> the provider
     * may return a <U>different</U> value every time this method is called
     * - e.g., if it is (re-)loading contents from a file.
     * @throws IOException              If failed to load the identity
     * @throws GeneralSecurityException If failed to parse the identity
     */
    KeyPair getClientIdentity() throws IOException, GeneralSecurityException;
}

package org.xbib.io.sshd.common.config.keys;

import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.OptionalFeature;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Represents an SSH key type.
 */
public interface Identity extends NamedResource, OptionalFeature {
    /**
     * @return The key algorithm - e.g., RSA, DSA, EC
     */
    String getAlgorithm();

    Class<? extends PublicKey> getPublicKeyType();

    Class<? extends PrivateKey> getPrivateKeyType();
}

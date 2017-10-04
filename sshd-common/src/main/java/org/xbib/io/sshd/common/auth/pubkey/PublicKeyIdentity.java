package org.xbib.io.sshd.common.auth.pubkey;

import java.security.PublicKey;

/**
 * Represents a public key identity.
 */
public interface PublicKeyIdentity {
    /**
     * @return The {@link PublicKey} identity value
     */
    PublicKey getPublicKey();

    /**
     * Proves the public key identity by signing the given data
     *
     * @param data Data to sign
     * @return Signed data - using the identity
     * @throws Exception If failed to sign the data
     */
    byte[] sign(byte[] data) throws Exception;
}
package org.xbib.io.sshd.common.signature;

import org.xbib.io.sshd.common.util.NumberUtils;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Signature interface for SSH used to sign or verify packets
 * Usually wraps a javax.crypto.Signature object.
 */
public interface Signature {
    /**
     * @return The signature algorithm name
     */
    String getAlgorithm();

    /**
     * @param key The {@link PublicKey} to be used for verifying signatures
     * @throws Exception If failed to initialize
     */
    void initVerifier(PublicKey key) throws Exception;

    /**
     * @param key The {@link PrivateKey} to be used for signing
     * @throws Exception If failed to initialize
     */
    void initSigner(PrivateKey key) throws Exception;

    /**
     * Update the computed signature with the given data
     *
     * @param hash The hash data buffer
     * @throws Exception If failed to update
     * @see #update(byte[], int, int)
     */
    default void update(byte[] hash) throws Exception {
        update(hash, 0, NumberUtils.length(hash));
    }

    /**
     * Update the computed signature with the given data
     *
     * @param hash The hash data buffer
     * @param off  Offset of hash data in buffer
     * @param len  Length of hash data
     * @throws Exception If failed to update
     */
    void update(byte[] hash, int off, int len) throws Exception;

    /**
     * Verify against the given signature
     *
     * @param sig The signed data
     * @return {@code true} if signature is valid
     * @throws Exception If failed to extract signed data for validation
     */
    boolean verify(byte[] sig) throws Exception;

    /**
     * Compute the signature
     *
     * @return The signature value
     * @throws Exception If failed to calculate the signature
     */
    byte[] sign() throws Exception;

}

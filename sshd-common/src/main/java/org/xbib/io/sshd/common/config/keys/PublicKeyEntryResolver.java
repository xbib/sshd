package org.xbib.io.sshd.common.config.keys;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

/**
 *
 */
@FunctionalInterface
public interface PublicKeyEntryResolver {
    /**
     * A resolver that ignores all input
     */
    PublicKeyEntryResolver IGNORING = new PublicKeyEntryResolver() {
        @Override
        public PublicKey resolve(String keyType, byte[] keyData) throws IOException, GeneralSecurityException {
            return null;
        }

        @Override
        public String toString() {
            return "IGNORING";
        }
    };

    /**
     * A resolver that fails on all input
     */
    PublicKeyEntryResolver FAILING = new PublicKeyEntryResolver() {
        @Override
        public PublicKey resolve(String keyType, byte[] keyData) throws IOException, GeneralSecurityException {
            throw new InvalidKeySpecException("Failing resolver on key type=" + keyType);
        }

        @Override
        public String toString() {
            return "FAILING";
        }
    };

    /**
     * @param keyType The {@code OpenSSH} reported key type
     * @param keyData The {@code OpenSSH} encoded key data
     * @return The extracted {@link PublicKey} - ignored if {@code null}
     * @throws IOException              If failed to parse the key data
     * @throws GeneralSecurityException If failed to generate the key
     */
    PublicKey resolve(String keyType, byte[] keyData) throws IOException, GeneralSecurityException;
}

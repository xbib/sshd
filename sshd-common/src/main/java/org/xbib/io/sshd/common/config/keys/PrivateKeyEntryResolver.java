package org.xbib.io.sshd.common.config.keys;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;

/**
 */
@FunctionalInterface
public interface PrivateKeyEntryResolver {
    /**
     * A resolver that ignores all input
     */
    PrivateKeyEntryResolver IGNORING = new PrivateKeyEntryResolver() {
        @Override
        public PrivateKey resolve(String keyType, byte[] keyData) throws IOException, GeneralSecurityException {
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
    PrivateKeyEntryResolver FAILING = new PrivateKeyEntryResolver() {
        @Override
        public PrivateKey resolve(String keyType, byte[] keyData) throws IOException, GeneralSecurityException {
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
     * @return The extracted {@link PrivateKey} - ignored if {@code null}
     * @throws IOException              If failed to parse the key data
     * @throws GeneralSecurityException If failed to generate the key
     */
    PrivateKey resolve(String keyType, byte[] keyData) throws IOException, GeneralSecurityException;
}

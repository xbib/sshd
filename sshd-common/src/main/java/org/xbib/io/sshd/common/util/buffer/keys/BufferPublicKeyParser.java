package org.xbib.io.sshd.common.util.buffer.keys;

import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collection;

/**
 * Parses a raw {@link PublicKey} from a {@link Buffer}
 *
 * @param <PUB> Type of {@link PublicKey} being extracted
 */
public interface BufferPublicKeyParser<PUB extends PublicKey> {

    BufferPublicKeyParser<PublicKey> EMPTY = new BufferPublicKeyParser<PublicKey>() {
        @Override
        public boolean isKeyTypeSupported(String keyType) {
            return false;
        }

        @Override
        public PublicKey getRawPublicKey(String keyType, Buffer buffer) throws GeneralSecurityException {
            throw new NoSuchAlgorithmException(keyType);
        }

        @Override
        public String toString() {
            return "EMPTY";
        }
    };

    BufferPublicKeyParser<PublicKey> DEFAULT = aggregate(
            Arrays.asList(
                    RSABufferPublicKeyParser.INSTANCE,
                    DSSBufferPublicKeyParser.INSTANCE,
                    ECBufferPublicKeyParser.INSTANCE,
                    ED25519BufferPublicKeyParser.INSTANCE));

    static BufferPublicKeyParser<PublicKey> aggregate(Collection<? extends BufferPublicKeyParser<? extends PublicKey>> parsers) {
        if (GenericUtils.isEmpty(parsers)) {
            return EMPTY;
        }

        return new BufferPublicKeyParser<PublicKey>() {
            @Override
            public boolean isKeyTypeSupported(String keyType) {
                for (BufferPublicKeyParser<? extends PublicKey> p : parsers) {
                    if (p.isKeyTypeSupported(keyType)) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public PublicKey getRawPublicKey(String keyType, Buffer buffer) throws GeneralSecurityException {
                for (BufferPublicKeyParser<? extends PublicKey> p : parsers) {
                    if (p.isKeyTypeSupported(keyType)) {
                        return p.getRawPublicKey(keyType, buffer);
                    }
                }

                throw new NoSuchAlgorithmException("No aggregate matcher for " + keyType);
            }

            @Override
            public String toString() {
                return String.valueOf(parsers);
            }
        };
    }

    /**
     * @param keyType The key type - e.g., &quot;ssh-rsa&quot;, &quot;ssh-dss&quot;
     * @return {@code true} if this key type is supported by the parser
     */
    boolean isKeyTypeSupported(String keyType);

    /**
     * @param keyType The key type - e.g., &quot;ssh-rsa&quot;, &quot;ssh-dss&quot;
     * @param buffer  The {@link Buffer} containing the encoded raw public key
     * @return The decoded {@link PublicKey}
     * @throws GeneralSecurityException If failed to generate the key
     */
    PUB getRawPublicKey(String keyType, Buffer buffer) throws GeneralSecurityException;
}

package org.xbib.io.sshd.common.config.keys;

import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.NumberUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.Objects;

/**
 * @param <PUB> Type of {@link PublicKey}
 * @param <PRV> Type of {@link PrivateKey}
 */
public interface PrivateKeyEntryDecoder<PUB extends PublicKey, PRV extends PrivateKey>
        extends KeyEntryResolver<PUB, PRV>, PrivateKeyEntryResolver {

    @Override
    default PrivateKey resolve(String keyType, byte[] keyData) throws IOException, GeneralSecurityException {
        ValidateUtils.checkNotNullAndNotEmpty(keyType, "No key type provided");
        Collection<String> supported = getSupportedTypeNames();
        if ((GenericUtils.size(supported) > 0) && supported.contains(keyType)) {
            return decodePrivateKey(org.xbib.io.sshd.common.config.keys.FilePasswordProvider.EMPTY, keyData);
        }

        throw new InvalidKeySpecException("resolve(" + keyType + ") not in listed supported types: " + supported);
    }

    /**
     * @param passwordProvider The {@link org.xbib.io.sshd.common.config.keys.FilePasswordProvider} to use
     *                         in case the data is encrypted - may be {@code null} if no encrypted
     *                         data is expected
     * @param keyData          The key data bytes in {@code OpenSSH} format (after
     *                         BASE64 decoding) - ignored if {@code null}/empty
     * @return The decoded {@link PrivateKey} - or {@code null} if no data
     * @throws IOException              If failed to decode the key
     * @throws GeneralSecurityException If failed to generate the key
     */
    default PRV decodePrivateKey(org.xbib.io.sshd.common.config.keys.FilePasswordProvider passwordProvider, byte... keyData)
            throws IOException, GeneralSecurityException {
        return decodePrivateKey(passwordProvider, keyData, 0, NumberUtils.length(keyData));
    }

    default PRV decodePrivateKey(org.xbib.io.sshd.common.config.keys.FilePasswordProvider passwordProvider, byte[] keyData, int offset, int length)
            throws IOException, GeneralSecurityException {
        if (length <= 0) {
            return null;
        }

        try (InputStream stream = new ByteArrayInputStream(keyData, offset, length)) {
            return decodePrivateKey(passwordProvider, stream);
        }
    }

    default PRV decodePrivateKey(org.xbib.io.sshd.common.config.keys.FilePasswordProvider passwordProvider, InputStream keyData)
            throws IOException, GeneralSecurityException {
        // the actual data is preceded by a string that repeats the key type
        String type = KeyEntryResolver.decodeString(keyData);
        if (GenericUtils.isEmpty(type)) {
            throw new StreamCorruptedException("Missing key type string");
        }

        Collection<String> supported = getSupportedTypeNames();
        if (GenericUtils.isEmpty(supported) || (!supported.contains(type))) {
            throw new InvalidKeySpecException("Reported key type (" + type + ") not in supported list: " + supported);
        }

        return decodePrivateKey(type, passwordProvider, keyData);
    }

    /**
     * @param keyType          The reported / encode key type
     * @param passwordProvider The {@link org.xbib.io.sshd.common.config.keys.FilePasswordProvider} to use
     *                         in case the data is encrypted - may be {@code null} if no encrypted
     *                         data is expected
     * @param keyData          The key data bytes stream positioned after the key type decoding
     *                         and making sure it is one of the supported types
     * @return The decoded {@link PrivateKey}
     * @throws IOException              If failed to read from the data stream
     * @throws GeneralSecurityException If failed to generate the key
     */
    PRV decodePrivateKey(String keyType, FilePasswordProvider passwordProvider, InputStream keyData)
            throws IOException, GeneralSecurityException;

    /**
     * Encodes the {@link PrivateKey} using the {@code OpenSSH} format - same
     * one used by the {@code decodePublicKey} method(s)
     *
     * @param s   The {@link OutputStream} to write the data to
     * @param key The {@link PrivateKey} - may not be {@code null}
     * @return The key type value - one of the {@link #getSupportedTypeNames()} or
     * {@code null} if encoding not supported
     * @throws IOException If failed to generate the encoding
     */
    default String encodePrivateKey(OutputStream s, PRV key) throws IOException {
        Objects.requireNonNull(key, "No private key provided");
        return null;
    }

    default boolean isPublicKeyRecoverySupported() {
        return false;
    }

    /**
     * Attempts to recover the public key given the private one
     *
     * @param prvKey The {@link PrivateKey}
     * @return The recovered {@link PublicKey} - {@code null} if cannot recover it
     * @throws GeneralSecurityException If failed to generate the public key
     */
    default PUB recoverPublicKey(PRV prvKey) throws GeneralSecurityException {
        return null;
    }
}

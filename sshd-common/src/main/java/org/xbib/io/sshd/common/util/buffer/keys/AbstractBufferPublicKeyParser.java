package org.xbib.io.sshd.common.util.buffer.keys;

import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.security.SecurityUtils;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * @param <PUB> Type of {@link PublicKey} being extracted
 */
public abstract class AbstractBufferPublicKeyParser<PUB extends PublicKey> implements BufferPublicKeyParser<PUB> {
    private final Class<PUB> keyClass;
    private final Collection<String> supported;

    protected AbstractBufferPublicKeyParser(Class<PUB> keyClass, String... supported) {
        this(keyClass, GenericUtils.isEmpty(supported) ? Collections.emptyList() : Arrays.asList(supported));
    }

    protected AbstractBufferPublicKeyParser(Class<PUB> keyClass, Collection<String> supported) {
        this.keyClass = Objects.requireNonNull(keyClass, "No key class");
        this.supported = ValidateUtils.checkNotNullAndNotEmpty(supported, "No supported types for %s", keyClass.getSimpleName());
    }

    public Collection<String> getSupportedKeyTypes() {
        return supported;
    }

    public final Class<PUB> getKeyClass() {
        return keyClass;
    }

    @Override
    public boolean isKeyTypeSupported(String keyType) {
        Collection<String> keys = getSupportedKeyTypes();
        return (GenericUtils.length(keyType) > 0)
                && (GenericUtils.size(keys) > 0)
                && keys.contains(keyType);
    }

    protected <S extends KeySpec> PUB generatePublicKey(String algorithm, S keySpec) throws GeneralSecurityException {
        KeyFactory keyFactory = getKeyFactory(algorithm);
        PublicKey key = keyFactory.generatePublic(keySpec);
        Class<PUB> kc = getKeyClass();
        if (!kc.isInstance(key)) {
            throw new InvalidKeySpecException("Mismatched generated key types: expected=" + kc.getSimpleName() + ", actual=" + key);
        }

        return kc.cast(key);
    }

    protected KeyFactory getKeyFactory(String algorithm) throws GeneralSecurityException {
        return SecurityUtils.getKeyFactory(algorithm);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " - supported=" + getSupportedKeyTypes();
    }
}

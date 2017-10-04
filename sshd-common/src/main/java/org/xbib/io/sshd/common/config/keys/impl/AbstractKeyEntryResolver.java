package org.xbib.io.sshd.common.config.keys.impl;

import org.xbib.io.sshd.common.config.keys.KeyEntryResolver;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.KeySpec;
import java.util.Collection;
import java.util.Objects;

/**
 * @param <PUB> Type of {@link PublicKey}
 * @param <PRV> Type of {@link PrivateKey}
 */
public abstract class AbstractKeyEntryResolver<PUB extends PublicKey, PRV extends PrivateKey>
        extends AbstractLoggingBean
        implements KeyEntryResolver<PUB, PRV> {
    private final Class<PUB> pubType;
    private final Class<PRV> prvType;
    private final Collection<String> names;

    protected AbstractKeyEntryResolver(Class<PUB> pubType, Class<PRV> prvType, Collection<String> names) {
        this.pubType = Objects.requireNonNull(pubType, "No public key type specified");
        this.prvType = Objects.requireNonNull(prvType, "No private key type specified");
        this.names = ValidateUtils.checkNotNullAndNotEmpty(names, "No type names provided");
    }

    @Override
    public final Class<PUB> getPublicKeyType() {
        return pubType;
    }

    @Override
    public final Class<PRV> getPrivateKeyType() {
        return prvType;
    }

    @Override
    public Collection<String> getSupportedTypeNames() {
        return names;
    }

    public PUB generatePublicKey(KeySpec keySpec) throws GeneralSecurityException {
        KeyFactory factory = getKeyFactoryInstance();
        Class<PUB> keyType = getPublicKeyType();
        return keyType.cast(factory.generatePublic(keySpec));
    }

    public PRV generatePrivateKey(KeySpec keySpec) throws GeneralSecurityException {
        KeyFactory factory = getKeyFactoryInstance();
        Class<PRV> keyType = getPrivateKeyType();
        return keyType.cast(factory.generatePrivate(keySpec));
    }

    @Override
    public String toString() {
        return getPublicKeyType().getSimpleName() + ": " + getSupportedTypeNames();
    }
}

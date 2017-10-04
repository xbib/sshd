package org.xbib.io.sshd.common.config.keys.impl;

import org.xbib.io.sshd.common.config.keys.PublicKeyEntryDecoder;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collection;

/**
 * Useful base class implementation for a decoder of an {@code OpenSSH} encoded key data
 *
 * @param <PUB> Type of {@link PublicKey}
 * @param <PRV> Type of {@link PrivateKey}
 */
public abstract class AbstractPublicKeyEntryDecoder<PUB extends PublicKey, PRV extends PrivateKey>
        extends AbstractKeyEntryResolver<PUB, PRV>
        implements PublicKeyEntryDecoder<PUB, PRV> {
    protected AbstractPublicKeyEntryDecoder(Class<PUB> pubType, Class<PRV> prvType, Collection<String> names) {
        super(pubType, prvType, names);
    }
}

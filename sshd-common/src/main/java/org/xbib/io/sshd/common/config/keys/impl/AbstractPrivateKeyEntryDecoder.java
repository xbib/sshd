package org.xbib.io.sshd.common.config.keys.impl;

import org.xbib.io.sshd.common.config.keys.PrivateKeyEntryDecoder;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collection;

/**
 * @param <PUB> Type of {@link PublicKey}
 * @param <PRV> Type of {@link PrivateKey}
 */
public abstract class AbstractPrivateKeyEntryDecoder<PUB extends PublicKey, PRV extends PrivateKey>
        extends AbstractKeyEntryResolver<PUB, PRV>
        implements PrivateKeyEntryDecoder<PUB, PRV> {
    protected AbstractPrivateKeyEntryDecoder(Class<PUB> pubType, Class<PRV> prvType, Collection<String> names) {
        super(pubType, prvType, names);
    }

}

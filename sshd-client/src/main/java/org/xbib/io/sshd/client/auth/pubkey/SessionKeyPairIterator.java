package org.xbib.io.sshd.client.auth.pubkey;

import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.common.signature.SignatureFactoriesManager;

import java.security.KeyPair;
import java.util.Iterator;

/**
 *
 */
public class SessionKeyPairIterator extends AbstractKeyPairIterator<KeyPairIdentity> {
    private final SignatureFactoriesManager signatureFactories;
    private final Iterator<KeyPair> keys;

    public SessionKeyPairIterator(ClientSession session, SignatureFactoriesManager signatureFactories, Iterator<KeyPair> keys) {
        super(session);
        this.signatureFactories = signatureFactories;   // OK if null
        this.keys = keys;   // OK if null
    }

    @Override
    public boolean hasNext() {
        return (keys != null) && keys.hasNext();
    }

    @Override
    public KeyPairIdentity next() {
        return new KeyPairIdentity(signatureFactories, getClientSession(), keys.next());
    }
}

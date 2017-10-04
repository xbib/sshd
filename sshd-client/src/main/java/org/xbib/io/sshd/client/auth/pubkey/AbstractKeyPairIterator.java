package org.xbib.io.sshd.client.auth.pubkey;

import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.client.session.ClientSessionHolder;
import org.xbib.io.sshd.common.auth.pubkey.PublicKeyIdentity;
import org.xbib.io.sshd.common.session.SessionHolder;

import java.util.Iterator;
import java.util.Objects;

/**
 * @param <I> Type of {@link PublicKeyIdentity} being iterated
 */
public abstract class AbstractKeyPairIterator<I extends PublicKeyIdentity>
        implements Iterator<I>, SessionHolder<ClientSession>, ClientSessionHolder {

    private final ClientSession session;

    protected AbstractKeyPairIterator(ClientSession session) {
        this.session = Objects.requireNonNull(session, "No session");
    }

    @Override
    public final ClientSession getClientSession() {
        return session;
    }

    @Override
    public final ClientSession getSession() {
        return getClientSession();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("No removal allowed");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getClientSession() + "]";
    }
}

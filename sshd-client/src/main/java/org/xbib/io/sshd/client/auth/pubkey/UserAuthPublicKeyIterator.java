package org.xbib.io.sshd.client.auth.pubkey;

import org.xbib.io.sshd.common.agent.SshAgent;
import org.xbib.io.sshd.common.agent.SshAgentFactory;
import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.auth.pubkey.KeyAgentIdentity;
import org.xbib.io.sshd.common.auth.pubkey.PublicKeyIdentity;
import org.xbib.io.sshd.common.keyprovider.KeyIdentityProvider;
import org.xbib.io.sshd.common.signature.SignatureFactoriesManager;
import org.xbib.io.sshd.common.util.GenericUtils;

import java.io.IOException;
import java.nio.channels.Channel;
import java.security.KeyPair;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.xbib.io.sshd.common.keyprovider.KeyIdentityProvider.EMPTY_KEYS_PROVIDER;
import static org.xbib.io.sshd.common.keyprovider.KeyIdentityProvider.resolveKeyIdentityProvider;

/**
 */
public class UserAuthPublicKeyIterator extends AbstractKeyPairIterator<PublicKeyIdentity> implements Channel {

    private final AtomicBoolean open = new AtomicBoolean(true);
    private Iterator<? extends PublicKeyIdentity> current;
    private SshAgent agent;

    public UserAuthPublicKeyIterator(ClientSession session, SignatureFactoriesManager signatureFactories) throws Exception {
        super(session);
        Collection<Stream<? extends PublicKeyIdentity>> identities = new LinkedList<>();
        FactoryManager manager = Objects.requireNonNull(session.getFactoryManager(), "No session factory manager");
        SshAgentFactory factory = manager.getAgentFactory();
        if (factory != null) {
            try {
                agent = Objects.requireNonNull(factory.createClient(manager), "No agent created");
                identities.add(agent.getIdentities().stream()
                        .map(kp -> new KeyAgentIdentity(agent, kp.getFirst(), kp.getSecond())));
            } catch (Exception e) {
                try {
                    closeAgent();
                } catch (Exception err) {
                    e.addSuppressed(err);
                }

                throw e;
            }
        }

        identities.add(Stream.of(providerOf(session))
                .map(KeyIdentityProvider::loadKeys)
                .flatMap(GenericUtils::stream)
                .map(kp -> new KeyPairIdentity(signatureFactories, session, kp)));

        current = identities.stream().flatMap(r -> r).iterator();
    }

    /**
     * Creates a &quot;unified&quot; {@link KeyIdentityProvider} of key pairs out of the registered
     * {@link KeyPair} identities and the extra available ones as a single iterator
     * of key pairs
     *
     * @param session The {@link ClientSession} - ignored if {@code null} (i.e., empty
     *                iterator returned)
     * @return The wrapping KeyIdentityProvider
     * @see ClientSession#getRegisteredIdentities()
     * @see ClientSession#getKeyPairProvider()
     */
    private static KeyIdentityProvider providerOf(ClientSession session) {
        return session == null
                ? EMPTY_KEYS_PROVIDER
                : resolveKeyIdentityProvider(session.getRegisteredIdentities(), session.getKeyPairProvider());
    }

    @Override
    public boolean hasNext() {
        if (!isOpen()) {
            return false;
        }

        return current.hasNext();
    }

    @Override
    public PublicKeyIdentity next() {
        if (!isOpen()) {
            throw new NoSuchElementException("Iterator is closed");
        }
        return current.next();
    }

    @Override
    public boolean isOpen() {
        return open.get();
    }

    @Override
    public void close() throws IOException {
        if (open.getAndSet(false)) {
            closeAgent();
        }
    }

    protected void closeAgent() throws IOException {
        if (agent != null) {
            try {
                agent.close();
            } finally {
                agent = null;
            }
        }
    }
}

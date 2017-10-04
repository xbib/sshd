package org.xbib.io.sshd.server.agent.local;

import org.xbib.io.sshd.common.agent.SshAgent;
import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.signature.BuiltinSignatures;
import org.xbib.io.sshd.common.signature.Signature;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.Pair;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.security.SecurityUtils;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A local SSH agent implementation
 */
public class AgentImpl implements SshAgent {

    private final List<Pair<KeyPair, String>> keys = new ArrayList<>();
    private final AtomicBoolean open = new AtomicBoolean(true);

    public AgentImpl() {
        super();
    }

    protected static Pair<KeyPair, String> getKeyPair(Collection<Pair<KeyPair, String>> keys, PublicKey key) {
        if (GenericUtils.isEmpty(keys) || (key == null)) {
            return null;
        }

        for (Pair<KeyPair, String> k : keys) {
            KeyPair kp = k.getFirst();
            if (KeyUtils.compareKeys(key, kp.getPublic())) {
                return k;
            }
        }

        return null;
    }

    @Override
    public boolean isOpen() {
        return open.get();
    }

    @Override
    public List<Pair<PublicKey, String>> getIdentities() throws IOException {
        if (!isOpen()) {
            throw new SshException("Agent closed");
        }

        return GenericUtils.map(keys, kp -> new Pair<>(kp.getFirst().getPublic(), kp.getSecond()));
    }

    @Override
    public byte[] sign(PublicKey key, byte[] data) throws IOException {
        if (!isOpen()) {
            throw new SshException("Agent closed");
        }

        try {
            Pair<KeyPair, String> pp = Objects.requireNonNull(getKeyPair(keys, key), "Key not found");
            KeyPair kp = ValidateUtils.checkNotNull(pp.getFirst(), "No key pair for agent=%s", pp.getSecond());
            PublicKey pubKey = ValidateUtils.checkNotNull(kp.getPublic(), "No public key for agent=%s", pp.getSecond());

            final Signature verif;
            if (pubKey instanceof DSAPublicKey) {
                verif = BuiltinSignatures.dsa.create();
            } else if (pubKey instanceof ECPublicKey) {
                ECPublicKey ecKey = (ECPublicKey) pubKey;
                verif = BuiltinSignatures.getByCurveSize(ecKey.getParams());
            } else if (pubKey instanceof RSAPublicKey) {
                verif = BuiltinSignatures.rsa.create();
            } else if (SecurityUtils.EDDSA.equalsIgnoreCase(pubKey.getAlgorithm())) {
                verif = BuiltinSignatures.ed25519.create();
            } else {
                throw new InvalidKeySpecException("Unsupported key type: " + pubKey.getClass().getSimpleName());
            }
            verif.initSigner(kp.getPrivate());
            verif.update(data);
            return verif.sign();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new SshException(e);
        }
    }

    @Override
    public void addIdentity(KeyPair key, String comment) throws IOException {
        if (!isOpen()) {
            throw new SshException("Agent closed");
        }
        keys.add(new Pair<>(Objects.requireNonNull(key, "No key"), comment));
    }

    @Override
    public void removeIdentity(PublicKey key) throws IOException {
        if (!isOpen()) {
            throw new SshException("Agent closed");
        }

        Pair<KeyPair, String> kp = getKeyPair(keys, key);
        if (kp == null) {
            throw new SshException("Key not found");
        }
        keys.remove(kp);
    }

    @Override
    public void removeAllIdentities() throws IOException {
        if (!isOpen()) {
            throw new SshException("Agent closed");
        }
        keys.clear();
    }

    @Override
    public void close() throws IOException {
        if (open.getAndSet(false)) {
            keys.clear();
        }
    }
}

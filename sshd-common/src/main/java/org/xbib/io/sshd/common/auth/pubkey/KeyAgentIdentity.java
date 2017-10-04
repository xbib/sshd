package org.xbib.io.sshd.common.auth.pubkey;

import org.xbib.io.sshd.common.agent.SshAgent;
import org.xbib.io.sshd.common.config.keys.KeyUtils;

import java.security.PublicKey;
import java.util.Objects;

/**
 * Uses an {@link SshAgent} to generate the identity signature.
 */
public class KeyAgentIdentity implements PublicKeyIdentity {
    private final SshAgent agent;
    private final PublicKey key;
    private final String comment;

    public KeyAgentIdentity(SshAgent agent, PublicKey key, String comment) {
        this.agent = Objects.requireNonNull(agent, "No signing agent");
        this.key = Objects.requireNonNull(key, "No public key");
        this.comment = comment;
    }

    @Override
    public PublicKey getPublicKey() {
        return key;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public byte[] sign(byte[] data) throws Exception {
        return agent.sign(getPublicKey(), data);
    }

    @Override
    public String toString() {
        PublicKey pubKey = getPublicKey();
        return getClass().getSimpleName() + "[" + KeyUtils.getKeyType(pubKey) + "]"
                + " fingerprint=" + KeyUtils.getFingerPrint(pubKey)
                + ", comment=" + getComment();
    }
}
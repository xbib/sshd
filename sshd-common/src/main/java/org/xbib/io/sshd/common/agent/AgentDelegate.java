package org.xbib.io.sshd.common.agent;

import org.xbib.io.sshd.common.agent.SshAgent;
import org.xbib.io.sshd.common.util.Pair;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.List;

/**
 *
 */
public class AgentDelegate implements SshAgent {

    private final SshAgent agent;

    public AgentDelegate(SshAgent agent) {
        this.agent = agent;
    }

    @Override
    public boolean isOpen() {
        return agent.isOpen();
    }

    @Override
    public void close() throws IOException {
        // ignored
    }

    @Override
    public List<Pair<PublicKey, String>> getIdentities() throws IOException {
        return agent.getIdentities();
    }

    @Override
    public byte[] sign(PublicKey key, byte[] data) throws IOException {
        return agent.sign(key, data);
    }

    @Override
    public void addIdentity(KeyPair key, String comment) throws IOException {
        agent.addIdentity(key, comment);
    }

    @Override
    public void removeIdentity(PublicKey key) throws IOException {
        agent.removeIdentity(key);
    }

    @Override
    public void removeAllIdentities() throws IOException {
        agent.removeAllIdentities();
    }
}

package org.xbib.io.sshd.common.agent;

import org.xbib.io.sshd.common.util.Pair;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.List;

/**
 * SSH key agent server.
 */
public interface SshAgent extends java.nio.channels.Channel {

    String SSH_AUTHSOCKET_ENV_NAME = "SSH_AUTH_SOCK";

    List<Pair<PublicKey, String>> getIdentities() throws IOException;

    byte[] sign(PublicKey key, byte[] data) throws IOException;

    void addIdentity(KeyPair key, String comment) throws IOException;

    void removeIdentity(PublicKey key) throws IOException;

    void removeAllIdentities() throws IOException;
}

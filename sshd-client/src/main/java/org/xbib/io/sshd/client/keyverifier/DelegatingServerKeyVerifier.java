package org.xbib.io.sshd.client.keyverifier;

import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

import java.net.SocketAddress;
import java.security.PublicKey;
import java.util.Map;

/*
 * A ServerKeyVerifier that delegates verification to the ServerKeyVerifier found in the ClientSession metadata
 * The ServerKeyVerifier can be specified at the SshClient level, which may have connections to multiple hosts.
 * This technique lets each connection have its own ServerKeyVerifier.
 *
 */
public class DelegatingServerKeyVerifier extends AbstractLoggingBean implements ServerKeyVerifier {
    public DelegatingServerKeyVerifier() {
        super();
    }

    @Override
    public boolean verifyServerKey(ClientSession sshClientSession, SocketAddress remoteAddress, PublicKey serverKey) {
        Map<Object, Object> metadataMap = sshClientSession.getMetadataMap();
        Object verifier = metadataMap.get(ServerKeyVerifier.class);
        if (verifier == null) {
            return true;
        }
        // We throw if it's not a ServerKeyVerifier...
        return ((ServerKeyVerifier) verifier).verifyServerKey(sshClientSession, remoteAddress, serverKey);
    }
}

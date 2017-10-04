package org.xbib.io.sshd.client.keyverifier;

import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

import java.net.SocketAddress;
import java.security.PublicKey;

/**
 * A ServerKeyVerifier that accepts one server key (specified in the constructor).
 */
public class RequiredServerKeyVerifier extends AbstractLoggingBean implements ServerKeyVerifier {
    private final PublicKey requiredKey;

    public RequiredServerKeyVerifier(PublicKey requiredKey) {
        this.requiredKey = requiredKey;
    }

    public final PublicKey getRequiredKey() {
        return requiredKey;
    }

    @Override
    public boolean verifyServerKey(ClientSession sshClientSession, SocketAddress remoteAddress, PublicKey serverKey) {
        if (requiredKey.equals(serverKey)) {
            return true;
        } else {
            return false;
        }
    }
}

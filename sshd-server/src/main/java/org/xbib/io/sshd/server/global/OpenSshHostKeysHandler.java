package org.xbib.io.sshd.server.global;

import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.RuntimeSshException;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.global.AbstractOpenSshHostKeysHandler;
import org.xbib.io.sshd.common.keyprovider.KeyPairProvider;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.signature.Signature;
import org.xbib.io.sshd.common.signature.SignatureFactoriesManager;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;
import org.xbib.io.sshd.common.util.buffer.keys.BufferPublicKeyParser;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * An initial handler for &quot;hostkeys-prove-00@openssh.com&quot; request.
 */
public class OpenSshHostKeysHandler extends AbstractOpenSshHostKeysHandler implements SignatureFactoriesManager {
    public static final String REQUEST = "hostkeys-prove-00@openssh.com";
    public static final OpenSshHostKeysHandler INSTANCE = new OpenSshHostKeysHandler() {
        @Override
        public List<NamedFactory<Signature>> getSignatureFactories() {
            return null;
        }

        @Override
        public void setSignatureFactories(List<NamedFactory<Signature>> factories) {
            if (!GenericUtils.isEmpty(factories)) {
                throw new UnsupportedOperationException("Not allowed to change default instance signature factories");
            }
        }
    };

    private List<NamedFactory<Signature>> factories;

    public OpenSshHostKeysHandler() {
        super(REQUEST);
    }

    public OpenSshHostKeysHandler(BufferPublicKeyParser<? extends PublicKey> parser) {
        super(REQUEST, parser);
    }

    @Override
    public List<NamedFactory<Signature>> getSignatureFactories() {
        return factories;
    }

    @Override
    public void setSignatureFactories(List<NamedFactory<Signature>> factories) {
        this.factories = factories;
    }

    @Override
    protected Result handleHostKeys(Session session, Collection<? extends PublicKey> keys, boolean wantReply, Buffer buffer) throws Exception {
        // according to the specification there MUST be reply required by the server
        ValidateUtils.checkTrue(wantReply, "No reply required for host keys of %s", session);
        Collection<? extends NamedFactory<Signature>> factories =
                ValidateUtils.checkNotNullAndNotEmpty(
                        SignatureFactoriesManager.resolveSignatureFactories(this, session),
                        "No signature factories available for host keys of session=%s",
                        session);

        // generate the required signatures
        buffer = session.createBuffer(SshConstants.SSH_MSG_REQUEST_SUCCESS);

        Buffer buf = new ByteArrayBuffer();
        byte[] sessionId = session.getSessionId();
        KeyPairProvider kpp = Objects.requireNonNull(session.getKeyPairProvider(), "No server keys provider");
        for (PublicKey k : keys) {
            String keyType = KeyUtils.getKeyType(k);
            Signature verifier = ValidateUtils.checkNotNull(
                    NamedFactory.create(factories, keyType),
                    "No signer could be located for key type=%s",
                    keyType);

            KeyPair kp;
            try {
                kp = ValidateUtils.checkNotNull(kpp.loadKey(keyType), "No key of type=%s available", keyType);
            } catch (Error e) {
                throw new RuntimeSshException(e);
            }
            verifier.initSigner(kp.getPrivate());

            buf.clear();
            buf.putString(REQUEST);
            buf.putBytes(sessionId);
            buf.putPublicKey(k);

            byte[] data = buf.getCompactData();
            verifier.update(data);

            byte[] signature = verifier.sign();
            buffer.putBytes(signature);
        }

        session.writePacket(buffer);
        return Result.Replied;
    }
}

package org.xbib.io.sshd.server.auth.pubkey;

import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.RuntimeSshException;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.signature.Signature;
import org.xbib.io.sshd.common.signature.SignatureFactoriesManager;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;
import org.xbib.io.sshd.server.auth.AbstractUserAuth;
import org.xbib.io.sshd.server.session.ServerSession;

import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Collection;
import java.util.List;

/**
 *
 *
 */
public class UserAuthPublicKey extends AbstractUserAuth implements SignatureFactoriesManager {
    public static final String NAME = UserAuthPublicKeyFactory.NAME;

    private List<NamedFactory<Signature>> factories;

    public UserAuthPublicKey() {
        this(null);
    }

    public UserAuthPublicKey(List<NamedFactory<Signature>> factories) {
        super(NAME);
        this.factories = factories; // OK if null/empty
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
    public Boolean doAuth(Buffer buffer, boolean init) throws Exception {
        ValidateUtils.checkTrue(init, "Instance not initialized");

        boolean hasSig = buffer.getBoolean();
        String alg = buffer.getString();
        int oldLim = buffer.wpos();
        int oldPos = buffer.rpos();
        int len = buffer.getInt();
        buffer.wpos(buffer.rpos() + len);

        ServerSession session = getServerSession();
        String username = getUsername();
        PublicKey key = buffer.getRawPublicKey();
        Collection<NamedFactory<Signature>> factories =
                ValidateUtils.checkNotNullAndNotEmpty(
                        SignatureFactoriesManager.resolveSignatureFactories(this, session),
                        "No signature factories for session=%s",
                        session);

        Signature verifier = ValidateUtils.checkNotNull(
                NamedFactory.create(factories, alg),
                "No verifier located for algorithm=%s",
                alg);
        verifier.initVerifier(key);
        buffer.wpos(oldLim);

        byte[] sig = hasSig ? buffer.getBytes() : null;
        PublickeyAuthenticator authenticator = session.getPublickeyAuthenticator();
        if (authenticator == null) {
            return Boolean.FALSE;
        }

        boolean authed;
        try {
            authed = authenticator.authenticate(username, key, session);
        } catch (Error e) {
            throw new RuntimeSshException(e);
        }

        if (!authed) {
            return Boolean.FALSE;
        }

        if (!hasSig) {
            sendPublicKeyResponse(session, username, alg, key, buffer.array(), oldPos, 4 + len, buffer);
            return null;
        }

        buffer.rpos(oldPos);
        buffer.wpos(oldPos + 4 + len);
        if (!verifySignature(session, getService(), getName(), username, alg, key, buffer, verifier, sig)) {
            throw new SignatureException("Key verification failed");
        }
        return Boolean.TRUE;
    }

    protected boolean verifySignature(ServerSession session, String service, String name, String username,
                                      String alg, PublicKey key, Buffer buffer, Signature verifier, byte[] sig) throws Exception {
        byte[] id = session.getSessionId();
        Buffer buf = new ByteArrayBuffer(id.length + username.length() + service.length() + name.length()
                + alg.length() + ByteArrayBuffer.DEFAULT_SIZE + Long.SIZE, false);
        buf.putBytes(id);
        buf.putByte(SshConstants.SSH_MSG_USERAUTH_REQUEST);
        buf.putString(username);
        buf.putString(service);
        buf.putString(name);
        buf.putBoolean(true);
        buf.putString(alg);
        buf.putBuffer(buffer);
        verifier.update(buf.array(), buf.rpos(), buf.available());
        return verifier.verify(sig);
    }

    protected void sendPublicKeyResponse(ServerSession session, String username, String alg, PublicKey key,
                                         byte[] keyBlob, int offset, int blobLen, Buffer buffer) throws Exception {

        Buffer buf = session.createBuffer(SshConstants.SSH_MSG_USERAUTH_PK_OK,
                GenericUtils.length(alg) + blobLen + Integer.SIZE);
        buf.putString(alg);
        buf.putRawBytes(keyBlob, offset, blobLen);
        session.writePacket(buf);
    }
}

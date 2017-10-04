package org.xbib.io.sshd.server.auth.hostbased;

import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.RuntimeSshException;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.signature.Signature;
import org.xbib.io.sshd.common.signature.SignatureFactoriesManager;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;
import org.xbib.io.sshd.common.util.security.SecurityUtils;
import org.xbib.io.sshd.server.auth.AbstractUserAuth;
import org.xbib.io.sshd.server.session.ServerSession;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class UserAuthHostBased extends AbstractUserAuth implements SignatureFactoriesManager {
    public static final String NAME = UserAuthHostBasedFactory.NAME;

    private List<NamedFactory<Signature>> factories;

    public UserAuthHostBased() {
        this(null);
    }

    public UserAuthHostBased(List<NamedFactory<Signature>> factories) {
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
    protected Boolean doAuth(Buffer buffer, boolean init) throws Exception {
        ValidateUtils.checkTrue(init, "Instance not initialized");

        int dataLen = buffer.available();
        String username = getUsername();
        ServerSession session = getSession();
        String keyType = buffer.getString();
        int keyLen = buffer.getInt();
        int keyOffset = buffer.rpos();

        Buffer buf = new ByteArrayBuffer(buffer.array(), keyOffset, keyLen, true);
        PublicKey clientKey = buf.getRawPublicKey();
        List<X509Certificate> certs = Collections.emptyList();
        if (buf.available() > 0) {
            CertificateFactory cf = SecurityUtils.getCertificateFactory("X.509");
            certs = new ArrayList<>();
            try (ByteArrayInputStream bais = new ByteArrayInputStream(buf.array(), buf.rpos(), buf.available())) {
                X509Certificate c = (X509Certificate) cf.generateCertificate(bais);
                certs.add(c);
            }
        }

        buffer.rpos(keyOffset + keyLen);
        String clientHostName = buffer.getString();
        String clientUsername = buffer.getString();

        byte[] signature = buffer.getBytes();

        HostBasedAuthenticator authenticator = session.getHostBasedAuthenticator();
        if (authenticator == null) {
            return Boolean.FALSE;
        }

        boolean authed;
        try {
            authed = authenticator.authenticate(session, username, clientKey, clientHostName, clientUsername, certs);
        } catch (Error e) {

            throw new RuntimeSshException(e);
        }

        if (!authed) {
            return Boolean.FALSE;
        }

        // verify signature
        Collection<NamedFactory<Signature>> factories =
                ValidateUtils.checkNotNullAndNotEmpty(
                        SignatureFactoriesManager.resolveSignatureFactories(this, session),
                        "No signature factories for session=%s",
                        session);
        Signature verifier = ValidateUtils.checkNotNull(
                NamedFactory.create(factories, keyType),
                "No verifier located for algorithm=%s",
                keyType);
        verifier.initVerifier(clientKey);

        byte[] id = session.getSessionId();
        buf = new ByteArrayBuffer(dataLen + id.length + Long.SIZE, false);
        buf.putBytes(id);
        buf.putByte(SshConstants.SSH_MSG_USERAUTH_REQUEST);
        buf.putString(username);
        buf.putString(getService());
        buf.putString(getName());
        buf.putString(keyType);
        buf.putInt(keyLen);
        // copy the key + certificates
        buf.putRawBytes(buffer.array(), keyOffset, keyLen);
        buf.putString(clientHostName);
        buf.putString(clientUsername);

        verifier.update(buf.array(), buf.rpos(), buf.available());
        if (!verifier.verify(signature)) {
            throw new Exception("Key verification failed");
        }

        return Boolean.TRUE;
    }
}

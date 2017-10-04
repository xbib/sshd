package org.xbib.io.sshd.client.auth.hostbased;

import org.xbib.io.sshd.client.auth.AbstractUserAuth;
import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.signature.Signature;
import org.xbib.io.sshd.common.signature.SignatureFactoriesManager;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.OsUtils;
import org.xbib.io.sshd.common.util.Pair;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.BufferUtils;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;
import org.xbib.io.sshd.common.util.net.SshdSocketAddress;

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class UserAuthHostBased extends AbstractUserAuth implements SignatureFactoriesManager {
    public static final String NAME = UserAuthHostBasedFactory.NAME;
    private final HostKeyIdentityProvider clientHostKeys;
    private Iterator<Pair<KeyPair, List<X509Certificate>>> keys;
    private List<NamedFactory<Signature>> factories;
    private String clientUsername;
    private String clientHostname;

    public UserAuthHostBased(HostKeyIdentityProvider clientHostKeys) {
        super(NAME);
        this.clientHostKeys = clientHostKeys;   // OK if null
    }

    @Override
    public void init(ClientSession session, String service) throws Exception {
        super.init(session, service);
        keys = HostKeyIdentityProvider.iteratorOf(clientHostKeys);  // in case multiple calls to the method
    }

    @Override
    public List<NamedFactory<Signature>> getSignatureFactories() {
        return factories;
    }

    @Override
    public void setSignatureFactories(List<NamedFactory<Signature>> factories) {
        this.factories = factories;
    }

    public String getClientUsername() {
        return clientUsername;
    }

    public void setClientUsername(String clientUsername) {
        this.clientUsername = clientUsername;
    }

    public String getClientHostname() {
        return clientHostname;
    }

    public void setClientHostname(String clientHostname) {
        this.clientHostname = clientHostname;
    }

    @Override
    protected boolean sendAuthDataRequest(ClientSession session, String service) throws Exception {
        String name = getName();
        if ((keys == null) || (!keys.hasNext())) {
            return false;
        }

        Pair<KeyPair, List<X509Certificate>> keyInfo = keys.next();
        KeyPair kp = keyInfo.getFirst();
        PublicKey pub = kp.getPublic();
        String keyType = KeyUtils.getKeyType(pub);

        Collection<NamedFactory<Signature>> factories =
                ValidateUtils.checkNotNullAndNotEmpty(
                        SignatureFactoriesManager.resolveSignatureFactories(this, session),
                        "No signature factories for session=%s",
                        session);
        Signature verifier = ValidateUtils.checkNotNull(
                NamedFactory.create(factories, keyType),
                "No signer could be located for key type=%s",
                keyType);

        byte[] id = session.getSessionId();
        String username = session.getUsername();
        String clientUsername = resolveClientUsername();
        String clientHostname = resolveClientHostname();

        Buffer buffer = session.createBuffer(SshConstants.SSH_MSG_USERAUTH_REQUEST,
                id.length + username.length() + service.length() + clientUsername.length() + clientHostname.length()
                        + keyType.length() + ByteArrayBuffer.DEFAULT_SIZE + Long.SIZE);
        buffer.clear();

        buffer.putRawPublicKey(pub);

        List<X509Certificate> certs = keyInfo.getSecond();
        if (GenericUtils.size(certs) > 0) {
            for (X509Certificate c : certs) {
                // TODO make sure this yields DER encoding
                buffer.putRawBytes(c.getEncoded());
            }
        }
        byte[] keyBytes = buffer.getCompactData();
        verifier.initSigner(kp.getPrivate());

        buffer = session.prepareBuffer(SshConstants.SSH_MSG_USERAUTH_REQUEST, BufferUtils.clear(buffer));
        buffer.putString(username);
        buffer.putString(service);
        buffer.putString(name);
        buffer.putString(keyType);
        buffer.putBytes(keyBytes);
        buffer.putString(clientHostname);
        buffer.putString(clientUsername);
        appendSignature(session, service, name, username, keyType, pub, keyBytes, clientHostname, clientUsername, verifier, buffer);
        session.writePacket(buffer);
        return true;
    }

    protected void appendSignature(ClientSession session, String service, String name, String username,
                                   String keyType, PublicKey key, byte[] keyBytes,
                                   String clientHostname, String clientUsername,
                                   Signature verifier, Buffer buffer) throws Exception {
        byte[] id = session.getSessionId();
        Buffer bs = new ByteArrayBuffer(id.length + username.length() + service.length() + name.length()
                + keyType.length() + keyBytes.length
                + clientHostname.length() + clientUsername.length()
                + ByteArrayBuffer.DEFAULT_SIZE + Long.SIZE, false);
        bs.putBytes(id);
        bs.putByte(SshConstants.SSH_MSG_USERAUTH_REQUEST);
        bs.putString(username);
        bs.putString(service);
        bs.putString(name);
        bs.putString(keyType);
        bs.putBytes(keyBytes);
        bs.putString(clientHostname);
        bs.putString(clientUsername);

        verifier.update(bs.array(), bs.rpos(), bs.available());
        byte[] signature = verifier.sign();

        bs.clear();

        bs.putString(keyType);
        bs.putBytes(signature);
        buffer.putBytes(bs.array(), bs.rpos(), bs.available());
    }

    @Override
    protected boolean processAuthDataRequest(ClientSession session, String service, Buffer buffer) throws Exception {
        int cmd = buffer.getUByte();
        throw new IllegalStateException("processAuthDataRequest(" + session + ")[" + service + "]"
                + " received unknown packet: cmd=" + SshConstants.getCommandMessageName(cmd));
    }

    protected String resolveClientUsername() {
        String value = getClientUsername();
        return GenericUtils.isEmpty(value) ? OsUtils.getCurrentUser() : value;
    }

    protected String resolveClientHostname() {
        String value = getClientHostname();
        if (GenericUtils.isEmpty(value)) {
            value = SshdSocketAddress.toAddressString(SshdSocketAddress.getFirstExternalNetwork4Address());
        }

        return GenericUtils.isEmpty(value) ? SshdSocketAddress.LOCALHOST_IP : value;
    }
}

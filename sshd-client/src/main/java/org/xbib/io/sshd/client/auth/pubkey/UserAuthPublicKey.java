package org.xbib.io.sshd.client.auth.pubkey;

import org.xbib.io.sshd.client.auth.AbstractUserAuth;
import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.RuntimeSshException;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.auth.pubkey.PublicKeyIdentity;
import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.signature.Signature;
import org.xbib.io.sshd.common.signature.SignatureFactoriesManager;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;

import java.io.Closeable;
import java.io.IOException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Iterator;
import java.util.List;

/**
 * Implements the &quot;publickey&quot; authentication mechanism.
 */
public class UserAuthPublicKey extends AbstractUserAuth implements SignatureFactoriesManager {
    public static final String NAME = UserAuthPublicKeyFactory.NAME;

    protected Iterator<PublicKeyIdentity> keys;
    protected PublicKeyIdentity current;
    protected List<NamedFactory<Signature>> factories;

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
    public void init(ClientSession session, String service) throws Exception {
        super.init(session, service);
        releaseKeys();  // just making sure in case multiple calls to the method
        try {
            keys = new UserAuthPublicKeyIterator(session, this);
        } catch (Error e) {
            throw new RuntimeSshException(e);
        }
    }

    @Override
    protected boolean sendAuthDataRequest(ClientSession session, String service) throws Exception {
        try {
            if ((keys == null) || (!keys.hasNext())) {
                return false;
            }

            current = keys.next();
        } catch (Error e) {
            throw new RuntimeSshException(e);
        }

        PublicKey key;
        try {
            key = current.getPublicKey();
        } catch (Error e) {

            throw new RuntimeSshException(e);
        }

        String algo = KeyUtils.getKeyType(key);
        String name = getName();

        Buffer buffer = session.createBuffer(SshConstants.SSH_MSG_USERAUTH_REQUEST);
        buffer.putString(session.getUsername());
        buffer.putString(service);
        buffer.putString(name);
        buffer.putBoolean(false);
        buffer.putString(algo);
        buffer.putPublicKey(key);
        session.writePacket(buffer);
        return true;
    }

    @Override
    protected boolean processAuthDataRequest(ClientSession session, String service, Buffer buffer) throws Exception {
        String name = getName();
        int cmd = buffer.getUByte();
        if (cmd != SshConstants.SSH_MSG_USERAUTH_PK_OK) {
            throw new IllegalStateException("processAuthDataRequest(" + session + ")[" + service + "][" + name + "]"
                    + " received unknown packet: cmd=" + SshConstants.getCommandMessageName(cmd));
        }

        /*
         * Make sure the server echo-ed the same key we sent as
         * sanctioned by RFC4252 section 7
         */
        PublicKey key;
        try {
            key = current.getPublicKey();
        } catch (Error e) {
            throw new RuntimeSshException(e);
        }

        String algo = KeyUtils.getKeyType(key);
        String rspKeyType = buffer.getString();
        if (!rspKeyType.equals(algo)) {
            throw new InvalidKeySpecException("processAuthDataRequest(" + session + ")[" + service + "][" + name + "]"
                    + " mismatched key types: expected=" + algo + ", actual=" + rspKeyType);
        }

        PublicKey rspKey = buffer.getPublicKey();
        if (!KeyUtils.compareKeys(rspKey, key)) {
            throw new InvalidKeySpecException("processAuthDataRequest(" + session + ")[" + service + "][" + name + "]"
                    + " mismatched " + algo + " keys: expected=" + KeyUtils.getFingerPrint(key) + ", actual=" + KeyUtils.getFingerPrint(rspKey));
        }

        String username = session.getUsername();
        buffer = session.createBuffer(SshConstants.SSH_MSG_USERAUTH_REQUEST,
                GenericUtils.length(username) + GenericUtils.length(service)
                        + GenericUtils.length(name) + GenericUtils.length(algo)
                        + ByteArrayBuffer.DEFAULT_SIZE + Long.SIZE);
        buffer.putString(username);
        buffer.putString(service);
        buffer.putString(name);
        buffer.putBoolean(true);
        buffer.putString(algo);
        buffer.putPublicKey(key);
        appendSignature(session, service, name, username, algo, key, buffer);

        session.writePacket(buffer);
        return true;
    }

    protected void appendSignature(ClientSession session, String service, String name, String username, String algo, PublicKey key, Buffer buffer) throws Exception {
        byte[] id = session.getSessionId();
        Buffer bs = new ByteArrayBuffer(id.length + username.length() + service.length() + name.length()
                + algo.length() + ByteArrayBuffer.DEFAULT_SIZE + Long.SIZE, false);
        bs.putBytes(id);
        bs.putByte(SshConstants.SSH_MSG_USERAUTH_REQUEST);
        bs.putString(username);
        bs.putString(service);
        bs.putString(name);
        bs.putBoolean(true);
        bs.putString(algo);
        bs.putPublicKey(key);

        byte[] contents = bs.getCompactData();
        byte[] sig;
        try {
            sig = current.sign(contents);
        } catch (Error e) {
            throw new RuntimeSshException(e);
        }
        bs.clear();
        bs.putString(algo);
        bs.putBytes(sig);
        buffer.putBytes(bs.array(), bs.rpos(), bs.available());
    }

    @Override
    public void destroy() {
        try {
            releaseKeys();
        } catch (IOException e) {
            throw new RuntimeException("Failed (" + e.getClass().getSimpleName() + ") to close agent: " + e.getMessage(), e);
        }

        super.destroy(); // for logging
    }

    protected void releaseKeys() throws IOException {
        try {
            if (keys instanceof Closeable) {
                ((Closeable) keys).close();
            }
        } finally {
            keys = null;
        }
    }
}

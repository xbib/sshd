package org.xbib.io.sshd.client.kex;

import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.kex.AbstractDH;
import org.xbib.io.sshd.common.kex.DHFactory;
import org.xbib.io.sshd.common.kex.KeyExchange;
import org.xbib.io.sshd.common.kex.KeyExchangeFactory;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.signature.Signature;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;

import java.util.Objects;

/**
 * Base class for DHG key exchange algorithms.
 * Implementations will only have to configure the required data on the
 * {@link org.xbib.io.sshd.common.kex.DHG} class in the {@link #getDH()} method.
 */
public class DHGClient extends AbstractDHClientKeyExchange {

    protected final DHFactory factory;
    protected AbstractDH dh;

    protected DHGClient(DHFactory factory) {
        this.factory = Objects.requireNonNull(factory, "No factory");
    }

    public static KeyExchangeFactory newFactory(final DHFactory delegate) {
        return new KeyExchangeFactory() {
            @Override
            public String getName() {
                return delegate.getName();
            }

            @Override
            public KeyExchange create() {
                return new DHGClient(delegate);
            }

            @Override
            public String toString() {
                return NamedFactory.class.getSimpleName()
                        + "<" + KeyExchange.class.getSimpleName() + ">"
                        + "[" + getName() + "]";
            }
        };
    }

    @Override
    public final String getName() {
        return factory.getName();
    }

    @Override
    public void init(Session s, byte[] v_s, byte[] v_c, byte[] i_s, byte[] i_c) throws Exception {
        super.init(s, v_s, v_c, i_s, i_c);
        dh = getDH();
        hash = dh.getHash();
        hash.init();
        e = dh.getE();
        Buffer buffer = s.createBuffer(SshConstants.SSH_MSG_KEXDH_INIT, e.length + Integer.SIZE);
        buffer.putMPInt(e);

        s.writePacket(buffer);
    }

    protected AbstractDH getDH() throws Exception {
        return factory.create();
    }

    @Override
    public boolean next(int cmd, Buffer buffer) throws Exception {
        Session session = getSession();
        if (cmd != SshConstants.SSH_MSG_KEXDH_REPLY) {
            throw new SshException(SshConstants.SSH2_DISCONNECT_KEY_EXCHANGE_FAILED,
                    "Protocol error: expected packet SSH_MSG_KEXDH_REPLY, got " + KeyExchange.getSimpleKexOpcodeName(cmd));
        }

        byte[] k_s = buffer.getBytes();
        f = buffer.getMPIntAsBytes();
        byte[] sig = buffer.getBytes();
        dh.setF(f);
        k = dh.getK();

        buffer = new ByteArrayBuffer(k_s);
        serverKey = buffer.getRawPublicKey();
        final String keyAlg = KeyUtils.getKeyType(serverKey);
        if (GenericUtils.isEmpty(keyAlg)) {
            throw new SshException("Unsupported server key type");
        }

        buffer = new ByteArrayBuffer();
        buffer.putBytes(v_c);
        buffer.putBytes(v_s);
        buffer.putBytes(i_c);
        buffer.putBytes(i_s);
        buffer.putBytes(k_s);
        buffer.putMPInt(e);
        buffer.putMPInt(f);
        buffer.putMPInt(k);
        hash.update(buffer.array(), 0, buffer.available());
        h = hash.digest();

        Signature verif = ValidateUtils.checkNotNull(NamedFactory.create(session.getSignatureFactories(), keyAlg),
                "No verifier located for algorithm=%s",
                keyAlg);
        verif.initVerifier(serverKey);
        verif.update(h);
        if (!verif.verify(sig)) {
            throw new SshException(SshConstants.SSH2_DISCONNECT_KEY_EXCHANGE_FAILED, "KeyExchange signature verification failed for key type=" + keyAlg);
        }
        return true;
    }
}

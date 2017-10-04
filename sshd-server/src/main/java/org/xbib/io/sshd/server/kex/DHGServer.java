package org.xbib.io.sshd.server.kex;

import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.kex.AbstractDH;
import org.xbib.io.sshd.common.kex.DHFactory;
import org.xbib.io.sshd.common.kex.KexProposalOption;
import org.xbib.io.sshd.common.kex.KeyExchange;
import org.xbib.io.sshd.common.kex.KeyExchangeFactory;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.signature.Signature;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.BufferUtils;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;
import org.xbib.io.sshd.server.session.ServerSession;

import java.security.KeyPair;
import java.util.Objects;

/**
 *
 */
public class DHGServer extends AbstractDHServerKeyExchange {

    protected final DHFactory factory;
    protected AbstractDH dh;

    protected DHGServer(DHFactory factory) {
        this.factory = Objects.requireNonNull(factory, "No factory");
    }

    public static KeyExchangeFactory newFactory(final DHFactory factory) {
        return new KeyExchangeFactory() {
            @Override
            public KeyExchange create() {
                return new DHGServer(factory);
            }

            @Override
            public String getName() {
                return factory.getName();
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
        dh = factory.create();
        hash = dh.getHash();
        hash.init();
        f = dh.getE();
    }

    @Override
    public boolean next(int cmd, Buffer buffer) throws Exception {
        ServerSession session = getServerSession();
        if (cmd != SshConstants.SSH_MSG_KEXDH_INIT) {
            throw new SshException(SshConstants.SSH2_DISCONNECT_KEY_EXCHANGE_FAILED,
                    "Protocol error: expected packet SSH_MSG_KEXDH_INIT, got " + KeyExchange.getSimpleKexOpcodeName(cmd));
        }

        e = buffer.getMPIntAsBytes();
        dh.setF(e);
        k = dh.getK();

        KeyPair kp = Objects.requireNonNull(session.getHostKey(), "No server key pair available");
        String algo = session.getNegotiatedKexParameter(KexProposalOption.SERVERKEYS);
        Signature sig = ValidateUtils.checkNotNull(
                NamedFactory.create(session.getSignatureFactories(), algo),
                "Unknown negotiated server keys: %s",
                algo);
        sig.initSigner(kp.getPrivate());

        buffer = new ByteArrayBuffer();
        buffer.putRawPublicKey(kp.getPublic());
        byte[] k_s = buffer.getCompactData();

        buffer.clear();
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
        sig.update(h);

        buffer.clear();
        buffer.putString(algo);
        byte[] sigBytes = sig.sign();
        buffer.putBytes(sigBytes);

        byte[] sigH = buffer.getCompactData();

        buffer = session.prepareBuffer(SshConstants.SSH_MSG_KEXDH_REPLY, BufferUtils.clear(buffer));
        buffer.putBytes(k_s);
        buffer.putBytes(f);
        buffer.putBytes(sigH);
        session.writePacket(buffer);
        return true;
    }
}

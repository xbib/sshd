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
import org.xbib.io.sshd.common.util.security.SecurityUtils;

import java.math.BigInteger;
import java.util.Objects;

/**
 *
 */
public class DHGEXClient extends AbstractDHClientKeyExchange {

    protected final DHFactory factory;
    protected byte expected;
    protected int min = SecurityUtils.MIN_DHGEX_KEY_SIZE;
    protected int prf;
    protected int max;
    protected AbstractDH dh;
    protected byte[] p;
    protected byte[] g;

    protected DHGEXClient(DHFactory factory) {
        this.factory = Objects.requireNonNull(factory, "No factory");
        this.max = SecurityUtils.getMaxDHGroupExchangeKeySize();
        this.prf = Math.min(SecurityUtils.PREFERRED_DHGEX_KEY_SIZE, max);
    }

    public static KeyExchangeFactory newFactory(final DHFactory delegate) {
        return new KeyExchangeFactory() {
            @Override
            public String getName() {
                return delegate.getName();
            }

            @Override
            public KeyExchange create() {
                return new DHGEXClient(delegate);
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
        Buffer buffer = s.createBuffer(SshConstants.SSH_MSG_KEX_DH_GEX_REQUEST, Integer.SIZE);
        buffer.putInt(min);
        buffer.putInt(prf);
        buffer.putInt(max);
        s.writePacket(buffer);

        expected = SshConstants.SSH_MSG_KEX_DH_GEX_GROUP;
    }

    @Override
    public boolean next(int cmd, Buffer buffer) throws Exception {
        Session session = getSession();
        if (cmd != expected) {
            throw new SshException(SshConstants.SSH2_DISCONNECT_KEY_EXCHANGE_FAILED,
                    "Protocol error: expected packet " + KeyExchange.getGroupKexOpcodeName(expected)
                            + ", got " + KeyExchange.getGroupKexOpcodeName(cmd));
        }

        if (cmd == SshConstants.SSH_MSG_KEX_DH_GEX_GROUP) {
            p = buffer.getMPIntAsBytes();
            g = buffer.getMPIntAsBytes();

            dh = getDH(new BigInteger(p), new BigInteger(g));
            hash = dh.getHash();
            hash.init();
            e = dh.getE();

            buffer = session.createBuffer(SshConstants.SSH_MSG_KEX_DH_GEX_INIT, e.length + Byte.SIZE);
            buffer.putMPInt(e);
            session.writePacket(buffer);
            expected = SshConstants.SSH_MSG_KEX_DH_GEX_REPLY;
            return false;
        }

        if (cmd == SshConstants.SSH_MSG_KEX_DH_GEX_REPLY) {
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
            buffer.putInt(min);
            buffer.putInt(prf);
            buffer.putInt(max);
            buffer.putMPInt(p);
            buffer.putMPInt(g);
            buffer.putMPInt(e);
            buffer.putMPInt(f);
            buffer.putMPInt(k);
            hash.update(buffer.array(), 0, buffer.available());
            h = hash.digest();

            Signature verif = ValidateUtils.checkNotNull(
                    NamedFactory.create(session.getSignatureFactories(), keyAlg),
                    "No verifier located for algorithm=%s",
                    keyAlg);
            verif.initVerifier(serverKey);
            verif.update(h);
            if (!verif.verify(sig)) {
                throw new SshException(SshConstants.SSH2_DISCONNECT_KEY_EXCHANGE_FAILED,
                        "KeyExchange signature verification failed for key type=" + keyAlg);
            }
            return true;
        }

        throw new IllegalStateException("Unknown command value: " + KeyExchange.getGroupKexOpcodeName(cmd));
    }

    protected AbstractDH getDH(BigInteger p, BigInteger g) throws Exception {
        return factory.create(p, g);
    }
}

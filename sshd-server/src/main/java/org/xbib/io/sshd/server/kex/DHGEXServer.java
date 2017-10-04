package org.xbib.io.sshd.server.kex;

import org.xbib.io.sshd.common.Factory;
import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.kex.DHFactory;
import org.xbib.io.sshd.common.kex.DHG;
import org.xbib.io.sshd.common.kex.DHGroupData;
import org.xbib.io.sshd.common.kex.KexProposalOption;
import org.xbib.io.sshd.common.kex.KeyExchange;
import org.xbib.io.sshd.common.kex.KeyExchangeFactory;
import org.xbib.io.sshd.common.random.Random;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.signature.Signature;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.BufferUtils;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;
import org.xbib.io.sshd.common.util.security.SecurityUtils;
import org.xbib.io.sshd.server.ServerFactoryManager;
import org.xbib.io.sshd.server.session.ServerSession;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 */
public class DHGEXServer extends AbstractDHServerKeyExchange {

    protected final DHFactory factory;
    protected DHG dh;
    protected int min;
    protected int prf;
    protected int max;
    protected byte expected;
    protected boolean oldRequest;

    protected DHGEXServer(DHFactory factory) {
        this.factory = Objects.requireNonNull(factory, "No factory");
    }

    public static KeyExchangeFactory newFactory(final DHFactory factory) {
        return new KeyExchangeFactory() {
            @Override
            public KeyExchange create() {
                return new DHGEXServer(factory);
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
        expected = SshConstants.SSH_MSG_KEX_DH_GEX_REQUEST;
    }

    @Override
    public boolean next(int cmd, Buffer buffer) throws Exception {
        ServerSession session = getServerSession();

        if (cmd == SshConstants.SSH_MSG_KEX_DH_GEX_REQUEST_OLD && expected == SshConstants.SSH_MSG_KEX_DH_GEX_REQUEST) {
            oldRequest = true;
            min = SecurityUtils.MIN_DHGEX_KEY_SIZE;
            prf = buffer.getInt();
            max = SecurityUtils.getMaxDHGroupExchangeKeySize();

            if (max < min || prf < min || max < prf) {
                throw new SshException(SshConstants.SSH2_DISCONNECT_KEY_EXCHANGE_FAILED,
                        "Protocol error: bad parameters " + min + " !< " + prf + " !< " + max);
            }
            dh = chooseDH(min, prf, max);
            f = dh.getE();
            hash = dh.getHash();
            hash.init();

            buffer = session.createBuffer(SshConstants.SSH_MSG_KEX_DH_GEX_GROUP);
            buffer.putMPInt(dh.getP());
            buffer.putMPInt(dh.getG());
            session.writePacket(buffer);

            expected = SshConstants.SSH_MSG_KEX_DH_GEX_INIT;
            return false;
        }

        if (cmd == SshConstants.SSH_MSG_KEX_DH_GEX_REQUEST && expected == SshConstants.SSH_MSG_KEX_DH_GEX_REQUEST) {
            min = buffer.getInt();
            prf = buffer.getInt();
            max = buffer.getInt();
            if (prf < min || max < prf) {
                throw new SshException(SshConstants.SSH2_DISCONNECT_KEY_EXCHANGE_FAILED,
                        "Protocol error: bad parameters " + min + " !< " + prf + " !< " + max);
            }
            dh = chooseDH(min, prf, max);
            f = dh.getE();
            hash = dh.getHash();
            hash.init();

            buffer = session.createBuffer(SshConstants.SSH_MSG_KEX_DH_GEX_GROUP);
            buffer.putMPInt(dh.getP());
            buffer.putMPInt(dh.getG());
            session.writePacket(buffer);

            expected = SshConstants.SSH_MSG_KEX_DH_GEX_INIT;
            return false;
        }

        if (cmd != expected) {
            throw new SshException(SshConstants.SSH2_DISCONNECT_KEY_EXCHANGE_FAILED,
                    "Protocol error: expected packet " + KeyExchange.getGroupKexOpcodeName(expected)
                            + ", got " + KeyExchange.getGroupKexOpcodeName(cmd));
        }

        if (cmd == SshConstants.SSH_MSG_KEX_DH_GEX_INIT) {
            e = buffer.getMPIntAsBytes();
            dh.setF(e);
            k = dh.getK();


            byte[] k_s;
            KeyPair kp = Objects.requireNonNull(session.getHostKey(), "No server key pair available");
            String algo = session.getNegotiatedKexParameter(KexProposalOption.SERVERKEYS);
            Signature sig = ValidateUtils.checkNotNull(
                    NamedFactory.create(session.getSignatureFactories(), algo),
                    "Unknown negotiated server keys: %s",
                    algo);
            sig.initSigner(kp.getPrivate());

            buffer = new ByteArrayBuffer();
            buffer.putRawPublicKey(kp.getPublic());
            k_s = buffer.getCompactData();

            buffer.clear();
            buffer.putBytes(v_c);
            buffer.putBytes(v_s);
            buffer.putBytes(i_c);
            buffer.putBytes(i_s);
            buffer.putBytes(k_s);
            if (oldRequest) {
                buffer.putInt(prf);
            } else {
                buffer.putInt(min);
                buffer.putInt(prf);
                buffer.putInt(max);
            }
            buffer.putMPInt(dh.getP());
            buffer.putMPInt(dh.getG());
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
            buffer = session.prepareBuffer(SshConstants.SSH_MSG_KEX_DH_GEX_REPLY, BufferUtils.clear(buffer));
            buffer.putBytes(k_s);
            buffer.putBytes(f);
            buffer.putBytes(sigH);
            session.writePacket(buffer);
            return true;
        }

        return false;
    }

    private DHG chooseDH(int min, int prf, int max) throws Exception {
        List<Moduli.DhGroup> groups = loadModuliGroups();

        min = Math.max(min, SecurityUtils.MIN_DHGEX_KEY_SIZE);
        prf = Math.max(prf, SecurityUtils.MIN_DHGEX_KEY_SIZE);
        prf = Math.min(prf, SecurityUtils.getMaxDHGroupExchangeKeySize());
        max = Math.min(max, SecurityUtils.getMaxDHGroupExchangeKeySize());
        int bestSize = 0;
        List<Moduli.DhGroup> selected = new ArrayList<>();
        for (Moduli.DhGroup group : groups) {
            if (group.size < min || group.size > max) {
                continue;
            }
            if ((group.size > prf && group.size < bestSize) || (group.size > bestSize && bestSize < prf)) {
                bestSize = group.size;
                selected.clear();
            }
            if (group.size == bestSize) {
                selected.add(group);
            }
        }

        ServerSession session = getServerSession();
        if (selected.isEmpty()) {
            return getDH(new BigInteger(DHGroupData.getP1()), new BigInteger(DHGroupData.getG()));
        }

        FactoryManager manager = Objects.requireNonNull(session.getFactoryManager(), "No factory manager");
        Factory<Random> factory = Objects.requireNonNull(manager.getRandomFactory(), "No random factory");
        Random random = Objects.requireNonNull(factory.create(), "No random generator");
        int which = random.random(selected.size());
        Moduli.DhGroup group = selected.get(which);
        return getDH(group.p, group.g);
    }

    protected List<Moduli.DhGroup> loadModuliGroups() throws IOException {
        ServerSession session = getServerSession();
        String moduliStr = session.getString(ServerFactoryManager.MODULI_URL);

        List<Moduli.DhGroup> groups = null;
        URL moduli;
        if (!GenericUtils.isEmpty(moduliStr)) {
            try {
                moduli = new URL(moduliStr);
                groups = Moduli.parseModuli(moduli);
            } catch (IOException e) {
            }
        }

        if (groups == null) {
            moduliStr = "/org/apache/sshd/moduli";
            try {
                moduli = getClass().getResource(moduliStr);
                if (moduli == null) {
                    throw new FileNotFoundException("Missing internal moduli file");
                }

                moduliStr = moduli.toExternalForm();
                groups = Moduli.parseModuli(moduli);
            } catch (IOException e) {
                throw e;    // this time we MUST throw the exception
            }
        }
        return groups;
    }

    protected DHG getDH(BigInteger p, BigInteger g) throws Exception {
        return (DHG) factory.create(p, g);
    }
}

package org.xbib.io.sshd.client.kex;

import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.client.session.ClientSessionHolder;
import org.xbib.io.sshd.common.kex.dh.AbstractDHKeyExchange;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.ValidateUtils;

import java.security.PublicKey;

/**
 */
public abstract class AbstractDHClientKeyExchange extends AbstractDHKeyExchange implements ClientSessionHolder {
    protected PublicKey serverKey;

    protected AbstractDHClientKeyExchange() {
        super();
    }

    @Override
    public final ClientSession getClientSession() {
        return (ClientSession) getSession();
    }

    @Override
    public void init(Session s, byte[] v_s, byte[] v_c, byte[] i_s, byte[] i_c) throws Exception {
        super.init(s, v_s, v_c, i_s, i_c);
        ValidateUtils.checkInstanceOf(s, ClientSession.class, "Using a client side KeyExchange on a server: %s", s);
    }

    @Override
    public PublicKey getServerKey() {
        return serverKey;
    }
}

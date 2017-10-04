package org.xbib.io.sshd.server.kex;

import org.xbib.io.sshd.common.kex.dh.AbstractDHKeyExchange;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.server.session.ServerSession;
import org.xbib.io.sshd.server.session.ServerSessionHolder;

import java.security.PublicKey;
import java.util.Objects;

/**
 *
 */
public abstract class AbstractDHServerKeyExchange extends AbstractDHKeyExchange implements ServerSessionHolder {
    protected AbstractDHServerKeyExchange() {
        super();
    }

    @Override
    public final ServerSession getServerSession() {
        return (ServerSession) getSession();
    }

    @Override
    public void init(Session s, byte[] v_s, byte[] v_c, byte[] i_s, byte[] i_c) throws Exception {
        super.init(s, v_s, v_c, i_s, i_c);
        ValidateUtils.checkInstanceOf(s, ServerSession.class, "Using a server side KeyExchange on a client: %s", s);
    }

    @Override
    public PublicKey getServerKey() {
        ServerSession session = getServerSession();
        return Objects.requireNonNull(session.getHostKey(), "No server key pair available").getPublic();
    }
}

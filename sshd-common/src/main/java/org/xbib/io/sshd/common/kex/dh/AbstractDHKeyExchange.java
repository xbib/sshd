package org.xbib.io.sshd.common.kex.dh;

import org.xbib.io.sshd.common.digest.Digest;
import org.xbib.io.sshd.common.kex.KeyExchange;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.session.SessionHolder;
import org.xbib.io.sshd.common.session.helpers.AbstractSession;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

/**
 *
 */
public abstract class AbstractDHKeyExchange extends AbstractLoggingBean implements KeyExchange, SessionHolder<AbstractSession> {

    protected byte[] v_s;
    protected byte[] v_c;
    protected byte[] i_s;
    protected byte[] i_c;
    protected Digest hash;
    protected byte[] e;
    protected byte[] f;
    protected byte[] k;
    protected byte[] h;

    private AbstractSession session;

    protected AbstractDHKeyExchange() {
        super();
    }

    @Override
    public void init(Session s, byte[] v_s, byte[] v_c, byte[] i_s, byte[] i_c) throws Exception {
        this.session = ValidateUtils.checkInstanceOf(s, AbstractSession.class, "Not an abstract session: %s", s);
        this.v_s = ValidateUtils.checkNotNullAndNotEmpty(v_s, "No v_s value");
        this.v_c = ValidateUtils.checkNotNullAndNotEmpty(v_c, "No v_c value");
        this.i_s = ValidateUtils.checkNotNullAndNotEmpty(i_s, "No i_s value");
        this.i_c = ValidateUtils.checkNotNullAndNotEmpty(i_c, "No i_c value");
    }

    @Override
    public AbstractSession getSession() {
        return session;
    }

    @Override
    public Digest getHash() {
        return hash;
    }

    @Override
    public byte[] getH() {
        return h;
    }

    @Override
    public byte[] getK() {
        return k;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getName() + "]";
    }
}

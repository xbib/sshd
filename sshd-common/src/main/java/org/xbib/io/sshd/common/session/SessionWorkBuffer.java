package org.xbib.io.sshd.common.session;

import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;

import java.util.Objects;

/**
 *
 */
public class SessionWorkBuffer extends ByteArrayBuffer implements SessionHolder<Session> {
    private final Session session;

    public SessionWorkBuffer(Session session) {
        this.session = Objects.requireNonNull(session, "No session");
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public void clear(boolean wipeData) {
        throw new UnsupportedOperationException("Not allowed to clear session work buffer of " + getSession());
    }

    public void forceClear(boolean wipeData) {
        super.clear(wipeData);
    }
}

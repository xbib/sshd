package org.xbib.io.sshd.server.auth;

import org.xbib.io.sshd.common.util.buffer.Buffer;

/**
 *
 */
public class UserAuthNone extends AbstractUserAuth {
    public static final String NAME = UserAuthNoneFactory.NAME;

    public UserAuthNone() {
        super(NAME);
    }

    @Override
    public Boolean doAuth(Buffer buffer, boolean init) {
        return Boolean.TRUE;
    }
}

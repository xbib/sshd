package org.xbib.io.sshd.client.global;

import org.xbib.io.sshd.common.global.AbstractOpenSshHostKeysHandler;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.keys.BufferPublicKeyParser;

import java.security.PublicKey;
import java.util.Collection;

/**
 * A handler for the &quot;hostkeys-00@openssh.com&quot; request - for now, only
 * reads the presented host key. One can override the {@link #handleHostKeys(Session, Collection, boolean, Buffer)}
 * methods in order to do something with the keys.
 */
public class OpenSshHostKeysHandler extends AbstractOpenSshHostKeysHandler {
    public static final String REQUEST = "hostkeys-00@openssh.com";
    public static final OpenSshHostKeysHandler INSTANCE = new OpenSshHostKeysHandler();

    public OpenSshHostKeysHandler() {
        super(REQUEST);
    }

    public OpenSshHostKeysHandler(BufferPublicKeyParser<? extends PublicKey> parser) {
        super(REQUEST, parser);
    }

    @Override
    protected Result handleHostKeys(Session session, Collection<? extends PublicKey> keys, boolean wantReply, Buffer buffer) throws Exception {
        // according to the spec, no reply should be required
        ValidateUtils.checkTrue(!wantReply, "Unexpected reply required for the host keys of %s", session);
        return Result.Replied;
    }
}

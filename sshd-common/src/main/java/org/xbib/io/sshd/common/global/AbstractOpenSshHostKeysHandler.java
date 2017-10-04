package org.xbib.io.sshd.common.global;

import org.xbib.io.sshd.common.session.ConnectionService;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.session.helpers.AbstractConnectionServiceRequestHandler;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.keys.BufferPublicKeyParser;

import java.security.PublicKey;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;

/**
 *
 */
public abstract class AbstractOpenSshHostKeysHandler extends AbstractConnectionServiceRequestHandler {
    private final String request;
    private final BufferPublicKeyParser<? extends PublicKey> parser;

    protected AbstractOpenSshHostKeysHandler(String request) {
        this(request, BufferPublicKeyParser.DEFAULT);
    }

    protected AbstractOpenSshHostKeysHandler(String request, BufferPublicKeyParser<? extends PublicKey> parser) {
        this.request = ValidateUtils.checkNotNullAndNotEmpty(request, "No request identifier");
        this.parser = Objects.requireNonNull(parser, "No public keys extractor");
    }

    public final String getRequestName() {
        return request;
    }

    public BufferPublicKeyParser<? extends PublicKey> getPublicKeysParser() {
        return parser;
    }

    @Override
    public Result process(ConnectionService connectionService, String request, boolean wantReply, Buffer buffer) throws Exception {
        String expected = getRequestName();
        if (!expected.equals(request)) {
            return super.process(connectionService, request, wantReply, buffer);
        }

        Collection<PublicKey> keys = new LinkedList<>();
        BufferPublicKeyParser<? extends PublicKey> p = getPublicKeysParser();
        if (p != null) {
            while (buffer.available() > 0) {
                PublicKey key = buffer.getPublicKey(p);
                if (key != null) {
                    keys.add(key);
                }
            }
        }

        return handleHostKeys(connectionService.getSession(), keys, wantReply, buffer);
    }

    protected abstract Result handleHostKeys(Session session, Collection<? extends PublicKey> keys, boolean wantReply, Buffer buffer) throws Exception;

    @Override
    public String toString() {
        return getRequestName();
    }
}

package org.xbib.io.sshd.server.session;

import org.xbib.io.sshd.common.Service;
import org.xbib.io.sshd.common.auth.AbstractUserAuthServiceFactory;
import org.xbib.io.sshd.common.session.Session;

import java.io.IOException;

/**
 *
 */
public class ServerUserAuthServiceFactory extends AbstractUserAuthServiceFactory {
    public static final ServerUserAuthServiceFactory INSTANCE = new ServerUserAuthServiceFactory();

    public ServerUserAuthServiceFactory() {
        super();
    }

    @Override
    public Service create(Session session) throws IOException {
        return new ServerUserAuthService(session);
    }
}
package org.xbib.io.sshd.client.session;

import org.xbib.io.sshd.common.Service;
import org.xbib.io.sshd.common.auth.AbstractUserAuthServiceFactory;
import org.xbib.io.sshd.common.session.Session;

import java.io.IOException;

/**
 *
 */
public class ClientUserAuthServiceFactory extends AbstractUserAuthServiceFactory {
    public static final ClientUserAuthServiceFactory INSTANCE = new ClientUserAuthServiceFactory();

    public ClientUserAuthServiceFactory() {
        super();
    }

    @Override
    public Service create(Session session) throws IOException {
        return new ClientUserAuthService(session);
    }
}
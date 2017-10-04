package org.xbib.io.sshd.server.auth.password;

import org.xbib.io.sshd.server.auth.AbstractUserAuthFactory;

/**
 *
 */
public class UserAuthPasswordFactory extends AbstractUserAuthFactory {
    public static final String NAME = PASSWORD;
    public static final UserAuthPasswordFactory INSTANCE = new UserAuthPasswordFactory();

    public UserAuthPasswordFactory() {
        super(NAME);
    }

    @Override
    public UserAuthPassword create() {
        return new UserAuthPassword();
    }
}
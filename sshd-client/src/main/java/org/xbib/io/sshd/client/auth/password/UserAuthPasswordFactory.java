package org.xbib.io.sshd.client.auth.password;

import org.xbib.io.sshd.client.auth.AbstractUserAuthFactory;

/**
 */
public class UserAuthPasswordFactory extends AbstractUserAuthFactory {
    public static final String NAME = PASSWORD;
    public static final UserAuthPasswordFactory INSTANCE = new UserAuthPasswordFactory();

    public UserAuthPasswordFactory() {
        super(NAME);
    }

    @Override
    public org.xbib.io.sshd.client.auth.password.UserAuthPassword create() {
        return new UserAuthPassword();
    }
}
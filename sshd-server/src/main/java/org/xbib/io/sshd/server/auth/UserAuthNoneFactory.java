package org.xbib.io.sshd.server.auth;

/**
 *
 */
public class UserAuthNoneFactory extends AbstractUserAuthFactory {
    public static final String NAME = "none";
    public static final UserAuthNoneFactory INSTANCE = new UserAuthNoneFactory();

    public UserAuthNoneFactory() {
        super(NAME);
    }

    @Override
    public org.xbib.io.sshd.server.auth.UserAuthNone create() {
        return new UserAuthNone();
    }
}

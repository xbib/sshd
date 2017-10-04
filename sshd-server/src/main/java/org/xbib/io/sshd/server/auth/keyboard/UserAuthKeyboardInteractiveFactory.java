package org.xbib.io.sshd.server.auth.keyboard;

import org.xbib.io.sshd.server.auth.AbstractUserAuthFactory;

/**
 *
 */
public class UserAuthKeyboardInteractiveFactory extends AbstractUserAuthFactory {
    public static final String NAME = KB_INTERACTIVE;
    public static final UserAuthKeyboardInteractiveFactory INSTANCE = new UserAuthKeyboardInteractiveFactory();

    public UserAuthKeyboardInteractiveFactory() {
        super(NAME);
    }

    @Override
    public org.xbib.io.sshd.server.auth.keyboard.UserAuthKeyboardInteractive create() {
        return new UserAuthKeyboardInteractive();
    }
}
package org.xbib.io.sshd.client.auth.keyboard;

import org.xbib.io.sshd.client.auth.AbstractUserAuthFactory;

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
    public UserAuthKeyboardInteractive create() {
        return new UserAuthKeyboardInteractive();
    }
}
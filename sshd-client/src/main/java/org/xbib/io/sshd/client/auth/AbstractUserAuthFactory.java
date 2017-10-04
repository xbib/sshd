package org.xbib.io.sshd.client.auth;

import org.xbib.io.sshd.common.auth.AbstractUserAuthMethodFactory;

/**
 *
 */
public abstract class AbstractUserAuthFactory extends AbstractUserAuthMethodFactory<UserAuth> implements UserAuthFactory {
    protected AbstractUserAuthFactory(String name) {
        super(name);
    }
}

package org.xbib.io.sshd.common.auth;

import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

/**
 * @param <M> Type of user authentication method
 */
public abstract class AbstractUserAuthMethodFactory<M> extends AbstractLoggingBean implements UserAuthMethodFactory<M> {
    private final String name;

    protected AbstractUserAuthMethodFactory(String name) {
        this.name = ValidateUtils.checkNotNullAndNotEmpty(name, "No factory name provided");
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getName() + "]";
    }
}

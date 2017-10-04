package org.xbib.io.sshd.common.auth;

import org.xbib.io.sshd.common.ServiceFactory;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

/**
 *
 */
public abstract class AbstractUserAuthServiceFactory extends AbstractLoggingBean implements ServiceFactory {
    public static final String DEFAULT_NAME = "ssh-userauth";

    private final String name;

    protected AbstractUserAuthServiceFactory() {
        this(DEFAULT_NAME);
    }

    protected AbstractUserAuthServiceFactory(String name) {
        this.name = ValidateUtils.checkNotNullAndNotEmpty(name, "No factory name");
    }

    @Override
    public final String getName() {
        return name;
    }
}

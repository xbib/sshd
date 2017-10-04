package org.xbib.io.sshd.common.keyprovider;

import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

/**
 * Provides a default implementation for some {@link org.xbib.io.sshd.common.keyprovider.KeyPairProvider} methods
 */
public abstract class AbstractKeyPairProvider extends AbstractLoggingBean implements KeyPairProvider {
    protected AbstractKeyPairProvider() {
        super();
    }
}

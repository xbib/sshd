package org.xbib.io.sshd.common.util.closeable;

import org.xbib.io.sshd.common.Closeable;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

/**
 *
 */
public abstract class IoBaseCloseable extends AbstractLoggingBean implements Closeable {
    protected IoBaseCloseable() {
        this("");
    }

    protected IoBaseCloseable(String discriminator) {
        super(discriminator);
    }
}
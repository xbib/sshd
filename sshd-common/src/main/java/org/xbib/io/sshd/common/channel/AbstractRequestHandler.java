package org.xbib.io.sshd.common.channel;

import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

/**
 * @param <T> Request type
 */
public abstract class AbstractRequestHandler<T> extends AbstractLoggingBean implements RequestHandler<T> {
    protected AbstractRequestHandler() {
        super();
    }
}

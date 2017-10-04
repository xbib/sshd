package org.xbib.io.sshd.common.util.logging;

import org.xbib.io.sshd.common.util.GenericUtils;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Serves as a common base class for the vast majority of classes that require
 * some kind of logging. Facilitates quick and easy replacement of the actual used
 * logger from one framework to another
 */
public abstract class AbstractLoggingBean {
    protected final Logger log;
    private final AtomicReference<SimplifiedLog> simplifiedLog = new AtomicReference<>();

    /**
     * Default constructor - creates a logger using the full class name
     */
    protected AbstractLoggingBean() {
        this("");
    }

    /**
     * Create a logger for instances of the same class for which we might
     * want to have a &quot;discriminator&quot; for them
     *
     * @param discriminator The discriminator value - ignored if {@code null}
     *                      or empty
     */
    protected AbstractLoggingBean(String discriminator) {
        String name = getClass().getName();
        if (GenericUtils.length(discriminator) > 0) {
            name += "[" + discriminator + "]";
        }
        log = Logger.getLogger(name);
    }

    protected SimplifiedLog getSimplifiedLogger() {
        SimplifiedLog logger;
        synchronized (simplifiedLog) {
            logger = simplifiedLog.get();
            if (logger == null) {
                logger = LoggingUtils.wrap(log);
            }
        }

        return logger;
    }
}

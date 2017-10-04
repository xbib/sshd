package org.xbib.io.sshd.server;

import org.xbib.io.sshd.common.util.SshdEventListener;

/**
 * Define a listener to receive signals.
 */
@FunctionalInterface
public interface SignalListener extends SshdEventListener {

    static <L extends SignalListener> L validateListener(L listener) {
        return SshdEventListener.validateListener(listener, SignalListener.class.getSimpleName());
    }

    /**
     * @param signal The received {@link org.xbib.io.sshd.server.Signal}
     */
    void signal(Signal signal);
}

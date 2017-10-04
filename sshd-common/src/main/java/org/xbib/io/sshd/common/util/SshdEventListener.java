package org.xbib.io.sshd.common.util;

import java.lang.reflect.Proxy;
import java.util.EventListener;
import java.util.Objects;

/**
 *
 */
public interface SshdEventListener extends EventListener {

    /**
     * Makes sure that the listener is neither {@code null} nor a proxy
     *
     * @param <L>      Type of {@link SshdEventListener} being validation
     * @param listener The listener instance
     * @param prefix   Prefix text to be prepended to validation failure messages
     * @return The validated instance
     */
    static <L extends SshdEventListener> L validateListener(L listener, String prefix) {
        Objects.requireNonNull(listener, prefix + ": no instance");
        ValidateUtils.checkTrue(!Proxy.isProxyClass(listener.getClass()), prefix + ": proxies N/A");
        return listener;
    }
}

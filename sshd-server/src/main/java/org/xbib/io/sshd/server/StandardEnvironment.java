package org.xbib.io.sshd.server;

import org.xbib.io.sshd.common.channel.PtyMode;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 *
 */
public class StandardEnvironment extends AbstractLoggingBean implements Environment {
    private final Map<Signal, Collection<SignalListener>> listeners;
    private final Map<String, String> env;
    private final Map<PtyMode, Integer> ptyModes;

    public StandardEnvironment() {
        listeners = new ConcurrentHashMap<>(3);
        env = new ConcurrentHashMap<>();
        ptyModes = new ConcurrentHashMap<>();
    }

    @Override
    public void addSignalListener(SignalListener listener, Signal... signals) {
        addSignalListener(listener, Arrays.asList(ValidateUtils.checkNotNullAndNotEmpty(signals, "No signals")));
    }

    @Override
    public void addSignalListener(SignalListener listener) {
        addSignalListener(listener, Signal.SIGNALS);
    }

    /*
     * NOTE: we don't care if the collection is a Set or not - after all,
     * we hold the listeners inside a Set, so even if we add several times
     * the same listener to the same signal set, it is harmless
     */
    @Override
    public void addSignalListener(SignalListener listener, Collection<Signal> signals) {
        SignalListener.validateListener(listener);
        ValidateUtils.checkNotNullAndNotEmpty(signals, "No signals");

        for (Signal s : signals) {
            getSignalListeners(s, true).add(listener);
        }
    }

    @Override
    public Map<String, String> getEnv() {
        return env;
    }

    @Override
    public Map<PtyMode, Integer> getPtyModes() {
        return ptyModes;
    }

    @Override
    public void removeSignalListener(SignalListener listener) {
        if (listener == null) {
            return;
        }

        SignalListener.validateListener(listener);
        for (Signal s : Signal.SIGNALS) {
            Collection<SignalListener> ls = getSignalListeners(s, false);
            if (ls != null) {
                ls.remove(listener);
            }
        }
    }

    public void signal(Signal signal) {
        Collection<SignalListener> ls = getSignalListeners(signal, false);

        if (GenericUtils.isEmpty(ls)) {
            return;
        }

        for (SignalListener l : ls) {
            try {
                l.signal(signal);

            } catch (RuntimeException e) {
            }
        }
    }

    /**
     * Adds a variable to the environment. This method is called <code>set</code>
     * according to the name of the appropriate posix command <code>set</code>
     *
     * @param key   environment variable name - never {@code null}/empty
     * @param value environment variable value
     */
    public void set(String key, String value) {
        // TODO: listening for property changes would be nice too.
        getEnv().put(ValidateUtils.checkNotNullAndNotEmpty(key, "Empty environment variable name"), value);
    }

    /**
     * Retrieves the set of listeners registered for a signal
     *
     * @param signal The specified {@link Signal}
     * @param create If {@code true} and no current listeners are mapped then
     *               creates a new {@link Collection}
     * @return The {@link Collection} of listeners registered for the signal - may be
     * {@code null} in case {@code create} is {@code false}
     */
    protected Collection<SignalListener> getSignalListeners(Signal signal, boolean create) {
        Collection<SignalListener> ls = listeners.get(signal);
        if ((ls == null) && create) {
            synchronized (listeners) {
                ls = listeners.get(signal);
                if (ls == null) {
                    ls = new CopyOnWriteArraySet<>();
                    listeners.put(signal, ls);
                }
            }
        }

        return ls;
    }

    @Override
    public String toString() {
        return "env=" + getEnv() + ", modes=" + getPtyModes();
    }
}

package org.xbib.io.sshd.server;

import org.xbib.io.sshd.common.channel.PtyMode;

import java.util.Collection;
import java.util.Map;

/**
 * Interface providing access to the environment map and allowing the registration
 * of listeners for certain signals.
 */
public interface Environment {
    /**
     * Key for the user environment variable.
     */
    String ENV_USER = "USER";

    /**
     * Key for the lines environment variable. Specifies the number of
     * lines visible on the client side. {@link #ENV_LINES} and
     * {@link #ENV_COLUMNS} make up the console screen size.
     */
    String ENV_LINES = "LINES";

    /**
     * Key for the columns environment variable. Specifies the number of
     * columns visible on the client side. {@link #ENV_LINES} and
     * {@link #ENV_COLUMNS} make up the console screen size.
     */
    String ENV_COLUMNS = "COLUMNS";

    /**
     * Key for the term environment variable. Describes the terminal or
     * terminal emulation which is in use.
     */
    String ENV_TERM = "TERM";

    /**
     * Retrieve the environment map.
     *
     * @return the environment {@link Map} - never {@code null}
     */
    Map<String, String> getEnv();

    /**
     * Retrieve the PTY modes settings.
     *
     * @return the {@link Map} of {@link PtyMode}s - never {@code null}
     */
    Map<PtyMode, Integer> getPtyModes();

    /**
     * Add a qualified listener for the specific signals.
     *
     * @param listener the {@link SignalListener} to register
     * @param signal   the {@link org.xbib.io.sshd.server.Signal}s  the listener is interested in
     */
    void addSignalListener(SignalListener listener, org.xbib.io.sshd.server.Signal... signal);

    /**
     * Add a qualified listener for the specific signals.
     *
     * @param listener the {@link SignalListener} to register
     * @param signals  the {@link org.xbib.io.sshd.server.Signal}s the listener is interested in
     */
    void addSignalListener(SignalListener listener, Collection<Signal> signals);

    /**
     * Add a global listener for all signals.
     *
     * @param listener the {@link SignalListener} to register
     */
    void addSignalListener(SignalListener listener);

    /**
     * Remove a previously registered listener for all the signals it was registered.
     *
     * @param listener the {@link SignalListener} to remove
     */
    void removeSignalListener(SignalListener listener);
}

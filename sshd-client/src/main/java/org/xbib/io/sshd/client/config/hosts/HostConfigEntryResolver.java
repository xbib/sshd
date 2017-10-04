package org.xbib.io.sshd.client.config.hosts;

import java.io.IOException;

/**
 */
@FunctionalInterface
public interface HostConfigEntryResolver {

    /**
     * An &quot;empty&quot; implementation that does not resolve any entry
     */
    HostConfigEntryResolver EMPTY = new HostConfigEntryResolver() {
        @Override
        public HostConfigEntry resolveEffectiveHost(String host, int port, String username) throws IOException {
            return null;
        }

        @Override
        public String toString() {
            return "EMPTY";
        }
    };

    /**
     * Invoked when creating a new client session in order to allow for overriding
     * of the original parameters
     *
     * @param host     The requested host - never {@code null}/empty
     * @param port     The requested port
     * @param username The requested username
     * @return A {@link HostConfigEntry} for the actual target - {@code null} if use
     * original parameters. <B>Note:</B> if any identity files are attached to the
     * configuration then they must point to <U>existing</U> locations. This means
     * that any macros such as <code>~, %d, %h</code>, etc. must be resolved <U>prior</U>
     * to returning the value
     * @throws IOException If failed to resolve the configuration
     */
    HostConfigEntry resolveEffectiveHost(String host, int port, String username) throws IOException;
}

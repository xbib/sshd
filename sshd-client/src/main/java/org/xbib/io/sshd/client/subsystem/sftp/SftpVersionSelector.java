package org.xbib.io.sshd.client.subsystem.sftp;

import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.NumberUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 *
 */
@FunctionalInterface
public interface SftpVersionSelector {
    /**
     * An {@link SftpVersionSelector} that returns the current version
     */
    SftpVersionSelector CURRENT = new NamedVersionSelector("CURRENT", (session, current, available) -> current);

    /**
     * An {@link SftpVersionSelector} that returns the maximum available version
     */
    SftpVersionSelector MAXIMUM = new NamedVersionSelector("MAXIMUM", (session, current, available) ->
            GenericUtils.stream(available).mapToInt(Integer::intValue).max().orElse(current));

    /**
     * An {@link SftpVersionSelector} that returns the maximum available version
     */
    SftpVersionSelector MINIMUM = new NamedVersionSelector("MINIMUM", (session, current, available) ->
            GenericUtils.stream(available).mapToInt(Integer::intValue).min().orElse(current));

    /**
     * Creates a selector the always returns the requested (fixed version) regardless
     * of what the current or reported available versions are. If the requested version
     * is not reported as available then an exception will be eventually thrown by the
     * client during re-negotiation phase.
     *
     * @param version The requested version
     * @return The {@link SftpVersionSelector}
     */
    static SftpVersionSelector fixedVersionSelector(int version) {
        return new NamedVersionSelector(Integer.toString(version), (session, current, available) -> version);
    }

    /**
     * Selects a version in order of preference - if none of the preferred
     * versions is listed as available then an exception is thrown when the
     * {@link SftpVersionSelector#selectVersion(ClientSession, int, List)} method is invoked
     *
     * @param preferred The preferred versions in decreasing order of
     *                  preference (i.e., most preferred is 1st) - may not be {@code null}/empty
     * @return A {@link SftpVersionSelector} that attempts to select
     * the most preferred version that is also listed as available.
     */
    static SftpVersionSelector preferredVersionSelector(int... preferred) {
        return preferredVersionSelector(NumberUtils.asList(preferred));

    }

    /**
     * Selects a version in order of preference - if none of the preferred
     * versions is listed as available then an exception is thrown when the
     * {@link SftpVersionSelector#selectVersion(ClientSession, int, List)} method is invoked
     *
     * @param preferred The preferred versions in decreasing order of
     *                  preference (i.e., most preferred is 1st)
     * @return A {@link SftpVersionSelector} that attempts to select
     * the most preferred version that is also listed as available.
     */
    static SftpVersionSelector preferredVersionSelector(Iterable<? extends Number> preferred) {
        ValidateUtils.checkNotNullAndNotEmpty((Collection<?>) preferred, "Empty preferred versions");
        return new NamedVersionSelector(GenericUtils.join(preferred, ','), (session, current, available) -> StreamSupport.stream(preferred.spliterator(), false)
                .mapToInt(Number::intValue)
                .filter(v -> v == current || available.contains(v))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Preferred versions (" + preferred + ") not available: " + available)));
    }

    /**
     * @param session   The {@link ClientSession} through which the SFTP connection is made
     * @param current   The current version negotiated with the server
     * @param available Extra versions available - may be empty and/or contain only the current one
     * @return The new requested version - if same as current, then nothing is done
     */
    int selectVersion(ClientSession session, int current, List<Integer> available);

    class NamedVersionSelector implements SftpVersionSelector {
        private final String name;
        private final SftpVersionSelector selector;

        public NamedVersionSelector(String name, SftpVersionSelector selector) {
            this.name = name;
            this.selector = selector;
        }

        @Override
        public int selectVersion(ClientSession session, int current, List<Integer> available) {
            return selector.selectVersion(session, current, available);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}

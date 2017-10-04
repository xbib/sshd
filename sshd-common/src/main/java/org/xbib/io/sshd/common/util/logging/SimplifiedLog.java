package org.xbib.io.sshd.common.util.logging;

import java.util.logging.Level;

/**
 *
 */
public interface SimplifiedLog {

    /**
     * An &quot;empty&quot; {@link SimplifiedLog} that does nothing
     */
    SimplifiedLog EMPTY = new SimplifiedLog() {
        @Override
        public boolean isEnabled(Level level) {
            return false;
        }

        @Override
        public void log(Level level, Object message, Throwable t) {
            // ignored
        }

        @Override
        public String toString() {
            return "EMPTY";
        }
    };

    boolean isEnabled(Level level);

    default void log(Level level, Object message) {
        log(level, message, null);
    }

    void log(Level level, Object message, Throwable t);
}

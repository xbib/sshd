package org.xbib.io.sshd.client.config.hosts;

import org.xbib.io.sshd.common.util.GenericUtils;

import java.util.regex.Pattern;

/**
 * Represents a pattern definition in the <U>known_hosts</U> file.
 */
public class HostPatternValue {
    private Pattern pattern;
    private int port;
    private boolean negated;

    public HostPatternValue() {
        super();
    }

    public HostPatternValue(Pattern pattern, boolean negated) {
        this(pattern, 0, negated);
    }

    public HostPatternValue(Pattern pattern, int port, boolean negated) {
        this.pattern = pattern;
        this.port = port;
        this.negated = negated;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isNegated() {
        return negated;
    }

    public void setNegated(boolean negated) {
        this.negated = negated;
    }

    @Override
    public String toString() {
        Pattern p = getPattern();
        String purePattern = (p == null) ? null : p.pattern();
        StringBuilder sb = new StringBuilder(GenericUtils.length(purePattern) + Short.SIZE);
        if (isNegated()) {
            sb.append(HostPatternsHolder.NEGATION_CHAR_PATTERN);
        }

        int portValue = getPort();
        if (portValue > 0) {
            sb.append(HostPatternsHolder.NON_STANDARD_PORT_PATTERN_ENCLOSURE_START_DELIM);
        }
        sb.append(purePattern);
        if (portValue > 0) {
            sb.append(HostPatternsHolder.NON_STANDARD_PORT_PATTERN_ENCLOSURE_END_DELIM);
            sb.append(HostPatternsHolder.PORT_VALUE_DELIMITER);
            sb.append(portValue);
        }

        return sb.toString();
    }
}

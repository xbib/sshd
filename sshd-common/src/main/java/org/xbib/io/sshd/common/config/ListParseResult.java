package org.xbib.io.sshd.common.config;

import org.xbib.io.sshd.common.util.GenericUtils;

import java.util.List;

/**
 * Used to hold the result of parsing a list of value. Such result contains known
 * and unknown values - which are accessible via the respective {@link #getParsedValues()}
 * and {@link #getUnsupportedValues()} methods. <B>Note:</B> the returned {@link List}s may
 * be un-modifiable, so it is recommended to avoid attempting changing the, returned
 * list(s).
 *
 * @param <E> Type of list item
 */
public abstract class ListParseResult<E> {
    private final List<E> parsed;
    private final List<String> unsupported;

    protected ListParseResult(List<E> parsed, List<String> unsupported) {
        this.parsed = parsed;
        this.unsupported = unsupported;
    }

    /**
     * @return The {@link List} of successfully parsed value instances
     * in the <U>same order</U> as they were encountered during parsing
     */
    public final List<E> getParsedValues() {
        return parsed;
    }

    /**
     * @return A {@link List} of unknown/unsupported configuration values for
     * the factories
     */
    public List<String> getUnsupportedValues() {
        return unsupported;
    }

    @Override
    public String toString() {
        return "parsed=" + GenericUtils.join(getParsedValues(), ',')
                + ";unsupported=" + GenericUtils.join(getUnsupportedValues(), ',');
    }
}

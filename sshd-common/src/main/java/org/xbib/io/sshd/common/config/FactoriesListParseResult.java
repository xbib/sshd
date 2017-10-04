package org.xbib.io.sshd.common.config;

import org.xbib.io.sshd.common.Factory;

import java.util.List;

/**
 * @param <T> Result type
 * @param <F> Factory type
 */
public abstract class FactoriesListParseResult<T, F extends Factory<T>> extends ListParseResult<F> {
    protected FactoriesListParseResult(List<F> parsed, List<String> unsupported) {
        super(parsed, unsupported);
    }

    /**
     * @return The {@link List} of successfully parsed {@link Factory} instances
     * in the <U>same order</U> as they were encountered during parsing
     */
    public final List<F> getParsedFactories() {
        return getParsedValues();
    }

    /**
     * @return A {@link List} of unknown/unsupported configuration values for
     * the factories
     */
    public List<String> getUnsupportedFactories() {
        return getUnsupportedValues();
    }
}

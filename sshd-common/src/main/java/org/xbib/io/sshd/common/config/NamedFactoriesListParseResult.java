package org.xbib.io.sshd.common.config;

import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.util.GenericUtils;

import java.util.List;

/**
 * Holds the result of parsing a list of {@link NamedFactory}ies
 *
 * @param <T> Result type
 * @param <F> Factory type
 */
public abstract class NamedFactoriesListParseResult<T, F extends NamedFactory<T>>
        extends FactoriesListParseResult<T, F> {

    protected NamedFactoriesListParseResult(List<F> parsed, List<String> unsupported) {
        super(parsed, unsupported);
    }

    @Override
    public String toString() {
        return "parsed=" + NamedResource.getNames(getParsedFactories())
                + ";unknown=" + GenericUtils.join(getUnsupportedFactories(), ',');
    }
}
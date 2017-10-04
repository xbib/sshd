package org.xbib.io.sshd.common.config;

import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.util.GenericUtils;

import java.util.List;

/**
 * @param <R> Type of result {@link NamedResource}
 */
public abstract class NamedResourceListParseResult<R extends NamedResource> extends ListParseResult<R> {
    protected NamedResourceListParseResult(List<R> parsed, List<String> unsupported) {
        super(parsed, unsupported);
    }

    /**
     * @return The {@link List} of successfully parsed {@link NamedResource} instances
     * in the <U>same order</U> as they were encountered during parsing
     */
    public final List<R> getParsedResources() {
        return getParsedValues();
    }

    /**
     * @return A {@link List} of unknown/unsupported configuration values for
     * the resources
     */
    public List<String> getUnsupportedResources() {
        return getUnsupportedValues();
    }

    @Override
    public String toString() {
        return "parsed=" + NamedResource.getNames(getParsedResources())
                + ";unknown=" + GenericUtils.join(getUnsupportedResources(), ',');
    }
}

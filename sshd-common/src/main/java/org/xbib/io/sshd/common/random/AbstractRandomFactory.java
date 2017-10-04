package org.xbib.io.sshd.common.random;

import org.xbib.io.sshd.common.util.ValidateUtils;

/**
 *
 */
public abstract class AbstractRandomFactory implements RandomFactory {
    private final String name;

    protected AbstractRandomFactory(String name) {
        this.name = ValidateUtils.checkNotNullAndNotEmpty(name, "No name");
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }
}

package org.xbib.io.sshd.common.subsystem.sftp.extensions;

import org.xbib.io.sshd.common.util.ValidateUtils;

/**
 * @param <T> Parse result type
 */
public abstract class AbstractParser<T> implements ExtensionParser<T> {
    private final String name;

    protected AbstractParser(String name) {
        this.name = ValidateUtils.checkNotNullAndNotEmpty(name, "No extension name");
    }

    @Override
    public final String getName() {
        return name;
    }
}

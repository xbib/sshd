package org.xbib.io.sshd.common.compression;

import org.xbib.io.sshd.common.util.ValidateUtils;

/**
 *
 */
public abstract class BaseCompression implements Compression {
    private final String name;

    protected BaseCompression(String name) {
        this.name = ValidateUtils.checkNotNullAndNotEmpty(name, "No compression name");
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public boolean isCompressionExecuted() {
        return true;
    }

    @Override
    public String toString() {
        return getName();
    }
}

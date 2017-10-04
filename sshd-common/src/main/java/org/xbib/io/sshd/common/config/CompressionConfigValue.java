package org.xbib.io.sshd.common.config;

import org.xbib.io.sshd.common.compression.BuiltinCompressions;
import org.xbib.io.sshd.common.compression.Compression;
import org.xbib.io.sshd.common.compression.CompressionFactory;
import org.xbib.io.sshd.common.util.GenericUtils;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Provides a &quot;bridge&quot; between the configuration values and the
 * actual {@link org.xbib.io.sshd.common.NamedFactory} for the {@link Compression}.
 */
public enum CompressionConfigValue implements CompressionFactory {
    YES(BuiltinCompressions.zlib),
    NO(BuiltinCompressions.none),
    DELAYED(BuiltinCompressions.delayedZlib);

    public static final Set<CompressionConfigValue> VALUES =
            Collections.unmodifiableSet(EnumSet.allOf(CompressionConfigValue.class));

    private final CompressionFactory factory;

    CompressionConfigValue(CompressionFactory delegate) {
        factory = delegate;
    }

    public static CompressionConfigValue fromName(String n) {
        if (GenericUtils.isEmpty(n)) {
            return null;
        }

        for (CompressionConfigValue v : VALUES) {
            if (n.equalsIgnoreCase(v.name())) {
                return v;
            }
        }

        return null;
    }

    @Override
    public final String getName() {
        return factory.getName();
    }

    @Override
    public final Compression create() {
        return factory.create();
    }

    @Override
    public boolean isSupported() {
        return factory.isSupported();
    }

    @Override
    public final String toString() {
        return getName();
    }

    @Override
    public boolean isDelayed() {
        return factory.isDelayed();
    }

    @Override
    public boolean isCompressionExecuted() {
        return factory.isCompressionExecuted();
    }
}

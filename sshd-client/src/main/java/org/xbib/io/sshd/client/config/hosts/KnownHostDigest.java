package org.xbib.io.sshd.client.config.hosts;

import org.xbib.io.sshd.common.Factory;
import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.mac.BuiltinMacs;
import org.xbib.io.sshd.common.mac.Mac;
import org.xbib.io.sshd.common.util.ValidateUtils;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Available digesters for known hosts entries.
 */
public enum KnownHostDigest implements NamedFactory<Mac> {
    SHA1("1", BuiltinMacs.hmacsha1);

    public static final Set<KnownHostDigest> VALUES =
            Collections.unmodifiableSet(EnumSet.allOf(KnownHostDigest.class));

    private final String name;
    private final Factory<Mac> factory;

    KnownHostDigest(String name, Factory<Mac> factory) {
        this.name = ValidateUtils.checkNotNullAndNotEmpty(name, "No name");
        this.factory = Objects.requireNonNull(factory, "No factory");
    }

    public static KnownHostDigest fromName(String name) {
        return NamedResource.findByName(name, String.CASE_INSENSITIVE_ORDER, VALUES);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Mac create() {
        return factory.create();
    }
}

package org.xbib.io.sshd.client.config.keys;

import org.xbib.io.sshd.common.config.keys.FilePasswordProvider;
import org.xbib.io.sshd.common.config.keys.PublicKeyEntry;
import org.xbib.io.sshd.common.util.GenericUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 *
 */
public class DefaultClientIdentitiesWatcher extends BuiltinClientIdentitiesWatcher {
    public DefaultClientIdentitiesWatcher(ClientIdentityLoader loader, FilePasswordProvider provider) {
        this(loader, provider, true);
    }

    public DefaultClientIdentitiesWatcher(ClientIdentityLoader loader, FilePasswordProvider provider, boolean strict) {
        this(true, loader, provider, strict);
    }

    public DefaultClientIdentitiesWatcher(boolean supportedOnly, ClientIdentityLoader loader, FilePasswordProvider provider, boolean strict) {
        this(supportedOnly,
                GenericUtils.supplierOf(Objects.requireNonNull(loader, "No client identity loader")),
                GenericUtils.supplierOf(Objects.requireNonNull(provider, "No password provider")),
                strict);
    }

    public DefaultClientIdentitiesWatcher(Supplier<ClientIdentityLoader> loader, Supplier<FilePasswordProvider> provider) {
        this(loader, provider, true);
    }

    public DefaultClientIdentitiesWatcher(Supplier<ClientIdentityLoader> loader, Supplier<FilePasswordProvider> provider, boolean strict) {
        this(true, loader, provider, strict);
    }

    public DefaultClientIdentitiesWatcher(boolean supportedOnly,
                                          Supplier<ClientIdentityLoader> loader, Supplier<FilePasswordProvider> provider, boolean strict) {
        super(PublicKeyEntry.getDefaultKeysFolderPath(), supportedOnly, loader, provider, strict);
    }

    public static List<Path> getDefaultBuiltinIdentitiesPaths() {
        return getDefaultBuiltinIdentitiesPaths(PublicKeyEntry.getDefaultKeysFolderPath());
    }
}

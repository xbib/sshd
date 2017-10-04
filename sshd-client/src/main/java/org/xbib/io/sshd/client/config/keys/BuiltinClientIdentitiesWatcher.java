package org.xbib.io.sshd.client.config.keys;

import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.config.keys.BuiltinIdentities;
import org.xbib.io.sshd.common.config.keys.FilePasswordProvider;
import org.xbib.io.sshd.common.util.GenericUtils;

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 *
 */
public class BuiltinClientIdentitiesWatcher extends ClientIdentitiesWatcher {
    private final boolean supportedOnly;

    public BuiltinClientIdentitiesWatcher(Path keysFolder, boolean supportedOnly,
                                          ClientIdentityLoader loader, FilePasswordProvider provider, boolean strict) {
        this(keysFolder, NamedResource.getNameList(BuiltinIdentities.VALUES), supportedOnly, loader, provider, strict);
    }

    public BuiltinClientIdentitiesWatcher(Path keysFolder, Collection<String> ids, boolean supportedOnly,
                                          ClientIdentityLoader loader, FilePasswordProvider provider, boolean strict) {
        this(keysFolder, ids, supportedOnly,
                GenericUtils.supplierOf(Objects.requireNonNull(loader, "No client identity loader")),
                GenericUtils.supplierOf(Objects.requireNonNull(provider, "No password provider")),
                strict);
    }

    public BuiltinClientIdentitiesWatcher(Path keysFolder, boolean supportedOnly,
                                          Supplier<ClientIdentityLoader> loader, Supplier<FilePasswordProvider> provider, boolean strict) {
        this(keysFolder, NamedResource.getNameList(BuiltinIdentities.VALUES), supportedOnly, loader, provider, strict);
    }

    public BuiltinClientIdentitiesWatcher(Path keysFolder, Collection<String> ids, boolean supportedOnly,
                                          Supplier<ClientIdentityLoader> loader, Supplier<FilePasswordProvider> provider, boolean strict) {
        super(getBuiltinIdentitiesPaths(keysFolder, ids), loader, provider, strict);
        this.supportedOnly = supportedOnly;
    }

    public static List<Path> getDefaultBuiltinIdentitiesPaths(Path keysFolder) {
        return getBuiltinIdentitiesPaths(keysFolder, NamedResource.getNameList(BuiltinIdentities.VALUES));
    }

    public static List<Path> getBuiltinIdentitiesPaths(Path keysFolder, Collection<String> ids) {
        Objects.requireNonNull(keysFolder, "No keys folder");
        if (GenericUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }

        List<Path> paths = new ArrayList<>(ids.size());
        for (String id : ids) {
            String fileName = ClientIdentity.getIdentityFileName(id);
            paths.add(keysFolder.resolve(fileName));
        }

        return paths;
    }

    public final boolean isSupportedOnly() {
        return supportedOnly;
    }

    @Override
    public Iterable<KeyPair> loadKeys() {
        return isSupportedOnly() ? loadKeys(this::isSupported) : super.loadKeys();
    }

    private boolean isSupported(KeyPair kp) {
        BuiltinIdentities id = BuiltinIdentities.fromKeyPair(kp);
        if ((id != null) && id.isSupported()) {
            return true;
        }
        return false;
    }
}

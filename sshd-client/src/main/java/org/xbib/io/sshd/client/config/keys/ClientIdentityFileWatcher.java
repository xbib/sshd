package org.xbib.io.sshd.client.config.keys;

import org.xbib.io.sshd.common.config.keys.FilePasswordProvider;
import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.Pair;
import org.xbib.io.sshd.common.util.io.IoUtils;
import org.xbib.io.sshd.common.util.io.ModifiableFileWatcher;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A {@link ClientIdentityProvider} that watches a given key file re-loading
 * its contents if it is ever modified, deleted or (re-)created
 */
public class ClientIdentityFileWatcher extends ModifiableFileWatcher implements ClientIdentityProvider {
    private final AtomicReference<KeyPair> identityHolder = new AtomicReference<>(null);
    private final Supplier<ClientIdentityLoader> loaderHolder;
    private final Supplier<FilePasswordProvider> providerHolder;
    private final boolean strict;

    public ClientIdentityFileWatcher(Path path, ClientIdentityLoader loader, FilePasswordProvider provider) {
        this(path, loader, provider, true);
    }

    public ClientIdentityFileWatcher(Path path, ClientIdentityLoader loader, FilePasswordProvider provider, boolean strict) {
        this(path,
                GenericUtils.supplierOf(Objects.requireNonNull(loader, "No client identity loader")),
                GenericUtils.supplierOf(Objects.requireNonNull(provider, "No password provider")),
                strict);
    }

    public ClientIdentityFileWatcher(Path path, Supplier<ClientIdentityLoader> loader, Supplier<FilePasswordProvider> provider) {
        this(path, loader, provider, true);
    }

    public ClientIdentityFileWatcher(Path path, Supplier<ClientIdentityLoader> loader, Supplier<FilePasswordProvider> provider, boolean strict) {
        super(path);
        this.loaderHolder = Objects.requireNonNull(loader, "No client identity loader");
        this.providerHolder = Objects.requireNonNull(provider, "No password provider");
        this.strict = strict;
    }

    public final boolean isStrict() {
        return strict;
    }

    public final ClientIdentityLoader getClientIdentityLoader() {
        return loaderHolder.get();
    }

    public final FilePasswordProvider getFilePasswordProvider() {
        return providerHolder.get();
    }

    @Override
    public KeyPair getClientIdentity() throws IOException, GeneralSecurityException {
        if (checkReloadRequired()) {
            KeyPair kp = identityHolder.getAndSet(null);     // start fresh
            Path path = getPath();

            if (exists()) {
                KeyPair id = reloadClientIdentity(path);
                if (!KeyUtils.compareKeyPairs(kp, id)) {
                }

                updateReloadAttributes();
                identityHolder.set(id);
            }
        }

        return identityHolder.get();
    }

    protected KeyPair reloadClientIdentity(Path path) throws IOException, GeneralSecurityException {
        if (isStrict()) {
            Pair<String, Object> violation = KeyUtils.validateStrictKeyFilePermissions(path, IoUtils.EMPTY_LINK_OPTIONS);
            if (violation != null) {
                return null;
            }
        }

        String location = path.toString();
        ClientIdentityLoader idLoader = Objects.requireNonNull(getClientIdentityLoader(), "No client identity loader");
        if (idLoader.isValidLocation(location)) {
            KeyPair kp = idLoader.loadClientIdentity(location, Objects.requireNonNull(getFilePasswordProvider(), "No file password provider"));
            return kp;
        }
        return null;
    }
}

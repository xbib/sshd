package org.xbib.io.sshd.client.config.keys;

import org.xbib.io.sshd.common.config.keys.FilePasswordProvider;
import org.xbib.io.sshd.common.keyprovider.AbstractKeyPairProvider;
import org.xbib.io.sshd.common.keyprovider.KeyPairProvider;
import org.xbib.io.sshd.common.util.GenericUtils;

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Watches over a group of files that contains client identities.
 */
public class ClientIdentitiesWatcher extends AbstractKeyPairProvider implements KeyPairProvider {
    private final Collection<ClientIdentityProvider> providers;

    public ClientIdentitiesWatcher(Collection<? extends Path> paths,
                                   ClientIdentityLoader loader, FilePasswordProvider provider) {
        this(paths, loader, provider, true);
    }

    public ClientIdentitiesWatcher(Collection<? extends Path> paths,
                                   ClientIdentityLoader loader, FilePasswordProvider provider, boolean strict) {
        this(paths,
                GenericUtils.supplierOf(Objects.requireNonNull(loader, "No client identity loader")),
                GenericUtils.supplierOf(Objects.requireNonNull(provider, "No password provider")),
                strict);
    }

    public ClientIdentitiesWatcher(Collection<? extends Path> paths,
                                   Supplier<ClientIdentityLoader> loader, Supplier<FilePasswordProvider> provider) {
        this(paths, loader, provider, true);
    }

    public ClientIdentitiesWatcher(Collection<? extends Path> paths,
                                   Supplier<ClientIdentityLoader> loader, Supplier<FilePasswordProvider> provider, boolean strict) {
        this(buildProviders(paths, loader, provider, strict));
    }

    public ClientIdentitiesWatcher(Collection<ClientIdentityProvider> providers) {
        this.providers = providers;
    }

    public static List<ClientIdentityProvider> buildProviders(
            Collection<? extends Path> paths, ClientIdentityLoader loader, FilePasswordProvider provider, boolean strict) {
        return buildProviders(paths,
                GenericUtils.supplierOf(Objects.requireNonNull(loader, "No client identity loader")),
                GenericUtils.supplierOf(Objects.requireNonNull(provider, "No password provider")),
                strict);
    }

    public static List<ClientIdentityProvider> buildProviders(
            Collection<? extends Path> paths, Supplier<ClientIdentityLoader> loader, Supplier<FilePasswordProvider> provider, boolean strict) {
        if (GenericUtils.isEmpty(paths)) {
            return Collections.emptyList();
        }

        return GenericUtils.map(paths, p -> new ClientIdentityFileWatcher(p, loader, provider, strict));
    }

    @Override
    public Iterable<KeyPair> loadKeys() {
        return loadKeys(null);
    }

    protected Iterable<KeyPair> loadKeys(Predicate<? super KeyPair> filter) {
        return () -> {
            Stream<KeyPair> stream = safeMap(GenericUtils.stream(providers), this::doGetKeyPair);
            if (filter != null) {
                stream = stream.filter(filter);
            }
            return stream.iterator();
        };
    }

    /**
     * Performs a mapping operation on the stream, discarding any null values
     * returned by the mapper.
     *
     * @param <U>    Original type
     * @param <V>    Mapped type
     * @param stream Original values stream
     * @param mapper Mapper to target type
     * @return Mapped stream
     */
    protected <U, V> Stream<V> safeMap(Stream<U> stream, Function<? super U, ? extends V> mapper) {
        return stream.map(u -> Optional.ofNullable(mapper.apply(u)))
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    protected KeyPair doGetKeyPair(ClientIdentityProvider p) {
        try {
            KeyPair kp = p.getClientIdentity();
            if (kp == null) {
            }
            return kp;
        } catch (Throwable e) {
            return null;
        }
    }
}

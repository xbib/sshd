package org.xbib.io.sshd.common.keyprovider;

import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.io.IoUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * This host key provider loads private keys from the specified files. The
 * loading is <U>lazy</U> - i.e., a file is not loaded until it is actually
 * required. Once required though, its loaded {@link KeyPair} result is
 * <U>cached</U> and not re-loaded.
 */
public class FileKeyPairProvider extends AbstractResourceKeyPairProvider<Path> {
    private Collection<? extends Path> files;

    public FileKeyPairProvider() {
        super();
    }

    public FileKeyPairProvider(Path path) {
        this(Collections.singletonList(Objects.requireNonNull(path, "No path provided")));
    }

    public FileKeyPairProvider(Path... files) {
        this(Arrays.asList(files));
    }

    public FileKeyPairProvider(Collection<? extends Path> files) {
        this.files = files;
    }

    public Collection<? extends Path> getPaths() {
        return files;
    }

    public void setPaths(Collection<? extends Path> paths) {
        // use absolute path in order to have unique cache keys
        Collection<Path> resolved = GenericUtils.map(paths, Path::toAbsolutePath);
        resetCacheMap(resolved);
        files = resolved;
    }

    public void setFiles(Collection<File> files) {
        setPaths(GenericUtils.map(files, File::toPath));
    }

    @Override
    public Iterable<KeyPair> loadKeys() {
        return loadKeys(getPaths());
    }

    @Override
    protected KeyPair doLoadKey(Path resource) throws IOException, GeneralSecurityException {
        return super.doLoadKey((resource == null) ? null : resource.toAbsolutePath());
    }

    @Override
    protected InputStream openKeyPairResource(String resourceKey, Path resource) throws IOException {
        return Files.newInputStream(resource, IoUtils.EMPTY_OPEN_OPTIONS);
    }
}

package org.xbib.io.sshd.common.file.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class MockFileSystem extends FileSystem {
    private final AtomicBoolean open = new AtomicBoolean(true);
    private final String name;

    public MockFileSystem(String name) {
        this.name = name;
    }

    @Override
    public FileSystemProvider provider() {
        throw new UnsupportedOperationException("provider() N/A");
    }

    @Override
    public void close() throws IOException {
        if (open.getAndSet(false)) {
            return; // debug breakpoint
        }
    }

    @Override
    public boolean isOpen() {
        return open.get();
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public String getSeparator() {
        return File.separator;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.emptyList();
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return Collections.emptyList();
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return Collections.emptySet();
    }

    @Override
    public Path getPath(String first, String... more) {
        throw new UnsupportedOperationException("getPath(" + first + ") " + Arrays.toString(more));
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        throw new UnsupportedOperationException("getPathMatcher(" + syntaxAndPattern + ")");
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException("getUserPrincipalLookupService() N/A");
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new IOException("newWatchService() N/A");
    }

    @Override
    public String toString() {
        return name;
    }
}

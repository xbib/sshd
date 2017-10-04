package org.xbib.io.sshd.common.file.root;

import org.xbib.io.sshd.common.file.util.BaseFileSystem;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 *
 */
public class RootedFileSystem extends BaseFileSystem<RootedPath> {

    private final Path rootPath;
    private final FileSystem rootFs;

    public RootedFileSystem(RootedFileSystemProvider fileSystemProvider, Path root, Map<String, ?> env) {
        super(fileSystemProvider);
        this.rootPath = Objects.requireNonNull(root, "No root path");
        this.rootFs = root.getFileSystem();
    }

    public FileSystem getRootFileSystem() {
        return rootFs;
    }

    public Path getRoot() {
        return rootPath;
    }

    @Override
    public RootedFileSystemProvider provider() {
        return (RootedFileSystemProvider) super.provider();
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public boolean isOpen() {
        return rootFs.isOpen();
    }

    @Override
    public boolean isReadOnly() {
        return rootFs.isReadOnly();
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return rootFs.supportedFileAttributeViews();
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        return rootFs.getUserPrincipalLookupService();
    }

    @Override
    protected RootedPath create(String root, List<String> names) {
        return new RootedPath(this, root, names);
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return rootFs.getFileStores();
    }

    @Override
    public String toString() {
        return rootPath.toString();
    }
}

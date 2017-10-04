package org.xbib.io.sshd.common.file.root;

import org.xbib.io.sshd.common.file.util.BasePath;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;

/**
 *
 */
public class RootedPath extends BasePath<RootedPath, RootedFileSystem> {
    public RootedPath(RootedFileSystem fileSystem, String root, List<String> names) {
        super(fileSystem, root, names);
    }

    @Override
    public File toFile() {
        RootedPath absolute = toAbsolutePath();
        RootedFileSystem fs = getFileSystem();
        Path path = fs.getRoot();
        for (String n : absolute.names) {
            path = path.resolve(n);
        }
        return path.toFile();
    }

    @Override
    public RootedPath toRealPath(LinkOption... options) throws IOException {
        RootedPath absolute = toAbsolutePath();
        FileSystem fs = getFileSystem();
        FileSystemProvider provider = fs.provider();
        provider.checkAccess(absolute);
        return absolute;
    }
}

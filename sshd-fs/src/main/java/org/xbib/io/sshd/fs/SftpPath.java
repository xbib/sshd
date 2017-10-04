package org.xbib.io.sshd.fs;

import org.xbib.io.sshd.common.file.util.BasePath;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;

/**
 *
 */
public class SftpPath extends BasePath<SftpPath, SftpFileSystem> {
    public SftpPath(SftpFileSystem fileSystem, String root, List<String> names) {
        super(fileSystem, root, names);
    }

    @Override
    public SftpPath toRealPath(LinkOption... options) throws IOException {
        // TODO: handle links
        SftpPath absolute = toAbsolutePath();
        FileSystem fs = getFileSystem();
        FileSystemProvider provider = fs.provider();
        provider.checkAccess(absolute);
        return absolute;
    }
}

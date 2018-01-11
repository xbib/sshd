package org.xbib.io.sshd.fs;

import org.xbib.io.sshd.common.file.util.BasePath;

import java.io.IOException;
import java.nio.file.LinkOption;
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
        getFileSystem().provider().checkAccess(absolute);
        return absolute;
    }
}

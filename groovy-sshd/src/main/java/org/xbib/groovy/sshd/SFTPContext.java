package org.xbib.groovy.sshd;

import org.xbib.io.sshd.fs.SftpFileSystemProvider;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.util.HashMap;
import java.util.Map;

/**
 */
class SFTPContext {

    final FileSystem fileSystem;

    SFTPContext(URI uri, Map<String, ?> env) throws IOException {
        this.fileSystem = env != null ?
                new SftpFileSystemProvider().newFileSystem(uri, env) :
                new SftpFileSystemProvider().newFileSystem(uri, new HashMap<>());
    }

    void close() throws IOException {
        fileSystem.close();
    }
}

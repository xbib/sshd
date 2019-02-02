package org.xbib.groovy.sshd;

import org.apache.sshd.fs.SftpFileSystem;
import org.apache.sshd.fs.SftpFileSystemProvider;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 */
class SFTPContext {

    final SftpFileSystemProvider provider;

    final SftpFileSystem fileSystem;

    SFTPContext(URI uri, Map<String, ?> env) throws IOException {
        this.provider = new SftpFileSystemProvider();
        this.fileSystem = provider.newFileSystem(uri, env);
    }

    void close() throws IOException {
        fileSystem.close();
    }
}

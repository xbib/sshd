package org.xbib.groovy.sshd;

import org.apache.sshd.client.ClientBuilder;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.fs.SftpFileSystem;
import org.apache.sshd.fs.SftpFileSystemProvider;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 */
class SFTPContext {

    private final SshClient sshClient;

    final SftpFileSystemProvider provider;

    final SftpFileSystem fileSystem;

    SFTPContext(URI uri, Map<String, ?> env) throws IOException {
        this.sshClient = ClientBuilder.builder().build();
        Object object = env.get("workers");
        if (object instanceof Integer) {
            sshClient.setNioWorkers((Integer) object);
        } else if (object instanceof String) {
            sshClient.setNioWorkers(Integer.parseInt((String) object));
        } else {
            // we do not require a vast pool of threads
            sshClient.setNioWorkers(1);
        }
        sshClient.start();
        this.provider = new SftpFileSystemProvider(sshClient);
        this.fileSystem = provider.newFileSystem(uri, env);
    }

    void close() throws IOException {
        sshClient.stop();
        fileSystem.close();
    }
}

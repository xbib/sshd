package org.xbib.io.sshd.fs;

import org.junit.Ignore;
import org.xbib.io.sshd.client.SshClient;
import org.xbib.io.sshd.client.config.hosts.HostConfigEntryResolver;
import org.xbib.io.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.keyprovider.KeyPairProvider;
import org.xbib.io.sshd.common.subsystem.sftp.SftpException;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import static org.xbib.io.sshd.common.subsystem.sftp.SftpConstants.SSH_FX_FAILURE;

@Ignore
public class SftpClientTest {

    @Test
    public void testClient() throws Exception {
        SshClient client = SshClient.setUpDefaultClient();
        client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
        client.setHostConfigEntryResolver(HostConfigEntryResolver.EMPTY);
        client.setKeyPairProvider(KeyPairProvider.EMPTY_KEYPAIR_PROVIDER);
        client.start();
        try (ClientSession session = client.connect("test", "localhost", 22)
                .verify(5L, TimeUnit.SECONDS).getSession()) {
            session.addPasswordIdentity("test".toCharArray());
            session.auth().verify(5L, TimeUnit.SECONDS);
            try (SftpClient sftp = session.createSftpClient()) {
                testClient(client, sftp);
            }
        } finally {
            client.stop();
        }
    }

    private void testClient(FactoryManager manager, SftpClient sftp) throws Exception {
        Path targetPath = Paths.get("/var/tmp");
        String dir = targetPath.resolve("sftp-client-test").toString();
        try {
            sftp.mkdir(dir);
        } catch (SftpException e) {
            // SSH_FX_FAILURE may be: directory already exists
            if (e.getStatus() != SSH_FX_FAILURE) {
                throw e;
            }
        }
        String file ="test.dat";
        try (SftpClient.CloseableHandle h = sftp.open(file, EnumSet.of(SftpClient.OpenMode.Write, SftpClient.OpenMode.Create))) {
            byte[] d = "0123456789\n".getBytes(StandardCharsets.UTF_8);
            sftp.write(h, 0, d, 0, d.length);
            sftp.write(h, d.length, d, 0, d.length);
        }
    }
}

package org.xbib.io.sshd.server;

import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.xbib.io.sshd.server.shell.ProcessShellFactory;
import org.xbib.io.sshd.server.subsystem.sftp.SftpSubsystemFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MockSshSftpServer {

    private SshServer sshd;

    public MockSshSftpServer(String host, int port) {
        sshd = SshServer.setUpDefaultServer();
        sshd.setHost(host);
        sshd.setPort(port);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("build/hostkey.ser")));
        sshd.setPasswordAuthenticator((username, password, session) -> true);
        sshd.getProperties().put(SshServer.IDLE_TIMEOUT, String.valueOf(5000));
        CommandFactory myCommandFactory = command ->
                new ProcessShellFactory(command.split(" ")).create();
        sshd.setCommandFactory(myCommandFactory);
        sshd.setShellFactory(new ProcessShellFactory("/bin/bash"));
        List<NamedFactory<Command>> namedFactoryList = new ArrayList<>();
        namedFactoryList.add(new SftpSubsystemFactory());
        sshd.setSubsystemFactories(namedFactoryList);
    }

    public void start() {
        try {
            sshd.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            sshd.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sshd = null;
    }
}

package org.xbib.groovy.sshd

import groovy.util.logging.Log4j2
import org.junit.jupiter.api.Test
import org.xbib.io.sshd.eddsa.EdDSASecurityProvider

import java.nio.file.Files
import java.nio.file.Path
import java.security.Security

@Log4j2
class SFTPTest {

    static {
        Security.addProvider(new EdDSASecurityProvider());
    }

    @Test
    void testSFTP() {
        SFTP sftp = SFTP.newInstance("sftp://demo.wftpserver.com:2222",[username: 'demo', password: 'demo'.toCharArray()])
        log.info sftp.exists('/')
        sftp.each('/') { Path path ->
            log.info "{} {} {}", path, Files.isDirectory(path), Files.getLastModifiedTime(path)
        }
    }

}

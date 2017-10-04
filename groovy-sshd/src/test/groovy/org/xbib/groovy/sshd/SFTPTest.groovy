package org.xbib.groovy.sshd

import groovy.util.logging.Log4j2
import org.junit.Test

import java.nio.file.Files
import java.nio.file.Path

@Log4j2
class SFTPTest {

    @Test
    void testSFTP() {
        SFTP sftp = SFTP.newInstance("sftp://demo.wftpserver.com:2222",[username: 'demo-user', password: 'demo-user'.toCharArray()])
        log.info sftp.exists('/')
        sftp.each('/') { Path path ->
            log.info "{} {} {}", path, Files.isDirectory(path), Files.getLastModifiedTime(path)
        }
    }
}
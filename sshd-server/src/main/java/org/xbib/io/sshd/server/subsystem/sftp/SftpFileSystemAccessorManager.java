package org.xbib.io.sshd.server.subsystem.sftp;

/**
 *
 */
public interface SftpFileSystemAccessorManager {

    SftpFileSystemAccessor getFileSystemAccessor();

    void setFileSystemAccessor(SftpFileSystemAccessor accessor);
}

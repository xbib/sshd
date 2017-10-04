package org.xbib.io.sshd.client.subsystem.sftp.extensions;

import org.xbib.io.sshd.client.subsystem.sftp.SftpClient.Handle;

import java.io.IOException;

/**
 * Implements the &quot;copy-data&quot; extension
 *
 * @see <A HREF="http://tools.ietf.org/id/draft-ietf-secsh-filexfer-extensions-00.txt">DRAFT 00 section 7</A>
 */
public interface CopyDataExtension extends SftpClientExtension {
    void copyData(Handle readHandle, long readOffset, long readLength, Handle writeHandle, long writeOffset) throws IOException;
}

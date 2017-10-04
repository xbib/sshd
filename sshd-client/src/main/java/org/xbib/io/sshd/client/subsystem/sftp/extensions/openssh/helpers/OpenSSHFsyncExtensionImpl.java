package org.xbib.io.sshd.client.subsystem.sftp.extensions.openssh.helpers;

import org.xbib.io.sshd.client.subsystem.sftp.RawSftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClient.Handle;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.helpers.AbstractSftpClientExtension;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.openssh.OpenSSHFsyncExtension;
import org.xbib.io.sshd.common.subsystem.sftp.extensions.openssh.FsyncExtensionParser;
import org.xbib.io.sshd.common.util.NumberUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.io.IOException;
import java.util.Map;

/**
 */
public class OpenSSHFsyncExtensionImpl extends AbstractSftpClientExtension implements OpenSSHFsyncExtension {
    public OpenSSHFsyncExtensionImpl(SftpClient client, RawSftpClient raw, Map<String, byte[]> extensions) {
        super(FsyncExtensionParser.NAME, client, raw, extensions);
    }

    @Override
    public void fsync(Handle fileHandle) throws IOException {
        byte[] handle = fileHandle.getIdentifier();
        Buffer buffer = getCommandBuffer(Integer.BYTES + NumberUtils.length(handle));
        buffer.putBytes(handle);
        sendAndCheckExtendedCommandStatus(buffer);
    }
}

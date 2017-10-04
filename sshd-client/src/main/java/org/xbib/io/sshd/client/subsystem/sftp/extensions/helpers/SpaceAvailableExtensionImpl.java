package org.xbib.io.sshd.client.subsystem.sftp.extensions.helpers;

import org.xbib.io.sshd.client.subsystem.sftp.RawSftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.SpaceAvailableExtension;
import org.xbib.io.sshd.common.subsystem.sftp.SftpConstants;
import org.xbib.io.sshd.common.subsystem.sftp.extensions.SpaceAvailableExtensionInfo;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.Collection;

/**
 * Implements &quot;space-available&quot; extension.
 */
public class SpaceAvailableExtensionImpl extends AbstractSftpClientExtension implements SpaceAvailableExtension {
    public SpaceAvailableExtensionImpl(SftpClient client, RawSftpClient raw, Collection<String> extra) {
        super(SftpConstants.EXT_SPACE_AVAILABLE, client, raw, extra);
    }

    @Override
    public SpaceAvailableExtensionInfo available(String path) throws IOException {
        Buffer buffer = getCommandBuffer(path);
        buffer.putString(path);
        buffer = checkExtendedReplyBuffer(receive(sendExtendedCommand(buffer)));

        if (buffer == null) {
            throw new StreamCorruptedException("Missing extended reply data");
        }

        return new SpaceAvailableExtensionInfo(buffer);
    }
}

package org.xbib.io.sshd.client.subsystem.sftp.extensions;

import org.xbib.io.sshd.client.subsystem.sftp.RawSftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.subsystem.sftp.extensions.ParserUtils;

import java.util.Map;

/**
 */
public interface SftpClientExtensionFactory extends NamedResource {
    default SftpClientExtension create(SftpClient client, RawSftpClient raw) {
        Map<String, byte[]> extensions = client.getServerExtensions();
        return create(client, raw, extensions, ParserUtils.parse(extensions));
    }

    SftpClientExtension create(SftpClient client, RawSftpClient raw, Map<String, byte[]> extensions, Map<String, ?> parsed);
}

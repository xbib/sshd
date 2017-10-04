package org.xbib.io.sshd.client.subsystem.sftp;

import java.io.IOException;

/**
 *
 */
public interface SftpClientCreator {
    /**
     * Create an SFTP client from this session.
     *
     * @return The created {@link SftpClient}
     * @throws IOException if failed to create the client
     */
    default SftpClient createSftpClient() throws IOException {
        return createSftpClient(SftpVersionSelector.CURRENT);
    }

    /**
     * Creates an SFTP client using the specified version
     *
     * @param version The version to use - <B>Note:</B> if the specified
     *                version is not supported by the server then an exception
     *                will occur
     * @return The created {@link SftpClient}
     * @throws IOException If failed to create the client or use the specified version
     */
    default SftpClient createSftpClient(int version) throws IOException {
        return createSftpClient(SftpVersionSelector.fixedVersionSelector(version));
    }

    /**
     * Creates an SFTP client while allowing the selection of a specific version
     *
     * @param selector The {@link SftpVersionSelector} to use - <B>Note:</B>
     *                 if the server does not support versions re-negotiation then the
     *                 selector will be presented with only one &quot;choice&quot; - the
     *                 current version
     * @return The created {@link SftpClient}
     * @throws IOException If failed to create the client or re-negotiate
     */
    SftpClient createSftpClient(SftpVersionSelector selector) throws IOException;
}

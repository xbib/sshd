package org.xbib.io.sshd.client.subsystem.sftp.extensions;

import java.io.IOException;

/**
 * @see <A HREF="http://tools.ietf.org/wg/secsh/draft-ietf-secsh-filexfer/draft-ietf-secsh-filexfer-09.txt">DRAFT 09 - section 9.1.1</A>
 */
public interface MD5FileExtension extends SftpClientExtension {
    /**
     * @param path      The (remote) path
     * @param offset    The offset to start calculating the hash
     * @param length    The number of data bytes to calculate the hash on - if
     *                  greater than available, then up to whatever is available
     * @param quickHash A quick-hash of the 1st 2048 bytes - ignored if {@code null}/empty
     * @return The hash value if the quick hash matches (or {@code null}/empty), or
     * {@code null}/empty if the quick hash is provided and it does not match
     * @throws IOException If failed to calculate the hash
     */
    byte[] getHash(String path, long offset, long length, byte[] quickHash) throws IOException;
}

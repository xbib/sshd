package org.xbib.io.sshd.client.subsystem.sftp.extensions;

import org.xbib.io.sshd.client.subsystem.sftp.SftpClient.Handle;
import org.xbib.io.sshd.common.util.Pair;

import java.io.IOException;
import java.util.Collection;

/**
 * @see <A HREF="http://tools.ietf.org/wg/secsh/draft-ietf-secsh-filexfer/draft-ietf-secsh-filexfer-09.txt">DRAFT 09 - section 9.1.2</A>
 */
public interface CheckFileHandleExtension extends SftpClientExtension {
    /**
     * @param handle      Remote file {@link Handle} - must be a file and opened for read
     * @param algorithms  Hash algorithms in preferred order
     * @param startOffset Start offset of the hash
     * @param length      Length of data to hash - if zero then till EOF
     * @param blockSize   Input block size to calculate individual hashes - if
     *                    zero the <U>one</U> hash of <U>all</U> the data
     * @return A {@link Pair} where left=hash algorithm name, right=the calculated
     * hashes.
     * @throws IOException If failed to execute the command
     */
    Pair<String, Collection<byte[]>> checkFileHandle(Handle handle, Collection<String> algorithms, long startOffset, long length, int blockSize) throws IOException;

}

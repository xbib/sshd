package org.xbib.io.sshd.client.subsystem.sftp.extensions.helpers;

import org.xbib.io.sshd.client.subsystem.sftp.RawSftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.common.subsystem.sftp.SftpConstants;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.Pair;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.Collection;
import java.util.LinkedList;

/**
 *
 */
public abstract class AbstractCheckFileExtension extends AbstractSftpClientExtension {
    protected AbstractCheckFileExtension(String name, SftpClient client, RawSftpClient raw, Collection<String> extras) {
        super(name, client, raw, extras);
    }

    protected Pair<String, Collection<byte[]>> doGetHash(Object target, Collection<String> algorithms, long offset, long length, int blockSize) throws IOException {
        Buffer buffer = getCommandBuffer(target, Byte.MAX_VALUE);
        putTarget(buffer, target);
        buffer.putString(GenericUtils.join(algorithms, ','));
        buffer.putLong(offset);
        buffer.putLong(length);
        buffer.putInt(blockSize);

        buffer = checkExtendedReplyBuffer(receive(sendExtendedCommand(buffer)));
        if (buffer == null) {
            throw new StreamCorruptedException("Missing extended reply data");
        }

        String targetType = buffer.getString();
        if (String.CASE_INSENSITIVE_ORDER.compare(targetType, SftpConstants.EXT_CHECK_FILE) != 0) {
            throw new StreamCorruptedException("Mismatched reply type: expected=" + SftpConstants.EXT_CHECK_FILE + ", actual=" + targetType);
        }

        String algo = buffer.getString();
        Collection<byte[]> hashes = new LinkedList<>();
        while (buffer.available() > 0) {
            byte[] hashValue = buffer.getBytes();
            hashes.add(hashValue);
        }

        return new Pair<>(algo, hashes);
    }
}

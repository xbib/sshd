package org.xbib.io.sshd.client.subsystem.sftp.extensions.helpers;

import org.xbib.io.sshd.client.subsystem.sftp.RawSftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.NumberUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.Collection;

/**
 *
 */
public abstract class AbstractMD5HashExtension extends AbstractSftpClientExtension {
    protected AbstractMD5HashExtension(String name, SftpClient client, RawSftpClient raw, Collection<String> extras) {
        super(name, client, raw, extras);
    }

    protected byte[] doGetHash(Object target, long offset, long length, byte[] quickHash) throws IOException {
        Buffer buffer = getCommandBuffer(target, Long.SIZE + 2 * Long.BYTES + Integer.BYTES + NumberUtils.length(quickHash));
        String opcode = getName();
        putTarget(buffer, target);
        buffer.putLong(offset);
        buffer.putLong(length);
        buffer.putBytes((quickHash == null) ? GenericUtils.EMPTY_BYTE_ARRAY : quickHash);

        buffer = checkExtendedReplyBuffer(receive(sendExtendedCommand(buffer)));
        if (buffer == null) {
            throw new StreamCorruptedException("Missing extended reply data");
        }

        String targetType = buffer.getString();
        if (String.CASE_INSENSITIVE_ORDER.compare(targetType, opcode) != 0) {
            throw new StreamCorruptedException("Mismatched reply target type: expected=" + opcode + ", actual=" + targetType);
        }

        byte[] hashValue = buffer.getBytes();

        return hashValue;
    }
}

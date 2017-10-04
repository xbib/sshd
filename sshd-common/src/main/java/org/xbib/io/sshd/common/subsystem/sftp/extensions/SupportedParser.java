package org.xbib.io.sshd.common.subsystem.sftp.extensions;

import org.xbib.io.sshd.common.subsystem.sftp.SftpConstants;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;

import java.util.Collection;

/**
 * Parses the &quot;supported&quot; extension as defined in
 * <A HREF="http://tools.ietf.org/wg/secsh/draft-ietf-secsh-filexfer/draft-ietf-secsh-filexfer-05.txt">DRAFT 05 - section 4.4</A>.
 */
public class SupportedParser extends AbstractParser<SupportedParser.Supported> {
    public static final SupportedParser INSTANCE = new SupportedParser();

    public SupportedParser() {
        super(SftpConstants.EXT_SUPPORTED);
    }

    @Override
    public Supported parse(byte[] input, int offset, int len) {
        return parse(new ByteArrayBuffer(input, offset, len));
    }

    public Supported parse(Buffer buffer) {
        Supported sup = new Supported();
        sup.supportedAttributeMask = buffer.getInt();
        sup.supportedAttributeBits = buffer.getInt();
        sup.supportedOpenFlags = buffer.getInt();
        sup.supportedAccessMask = buffer.getInt();
        sup.maxReadSize = buffer.getInt();
        sup.extensionNames = buffer.getStringList(false);
        return sup;
    }

    /**
     */
    public static class Supported {
        public int supportedAttributeMask;
        public int supportedAttributeBits;
        public int supportedOpenFlags;
        public int supportedAccessMask;
        public int maxReadSize;
        public Collection<String> extensionNames;

        @Override
        public String toString() {
            return "attrsMask=0x" + Integer.toHexString(supportedAttributeMask)
                    + ",attrsBits=0x" + Integer.toHexString(supportedAttributeBits)
                    + ",openFlags=0x" + Integer.toHexString(supportedOpenFlags)
                    + ",accessMask=0x" + Integer.toHexString(supportedAccessMask)
                    + ",maxReadSize=" + maxReadSize
                    + ",extensions=" + extensionNames;
        }
    }
}

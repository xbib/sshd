package org.xbib.io.sshd.common.subsystem.sftp.extensions;

import org.xbib.io.sshd.common.subsystem.sftp.SftpConstants;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;

import java.util.Collection;

/**
 * Parses the &quot;supported2&quot; extension as defined in
 * <A HREF="https://tools.ietf.org/html/draft-ietf-secsh-filexfer-13#page-10">DRAFT 13 section 5.4</A>
 */
public class Supported2Parser extends AbstractParser<Supported2Parser.Supported2> {
    public static final Supported2Parser INSTANCE = new Supported2Parser();

    public Supported2Parser() {
        super(SftpConstants.EXT_SUPPORTED2);
    }

    @Override
    public Supported2 parse(byte[] input, int offset, int len) {
        return parse(new ByteArrayBuffer(input, offset, len));
    }

    public Supported2 parse(Buffer buffer) {
        Supported2 sup2 = new Supported2();
        sup2.supportedAttributeMask = buffer.getInt();
        sup2.supportedAttributeBits = buffer.getInt();
        sup2.supportedOpenFlags = buffer.getInt();
        sup2.supportedAccessMask = buffer.getInt();
        sup2.maxReadSize = buffer.getInt();
        sup2.supportedOpenBlockVector = buffer.getShort();
        sup2.supportedBlock = buffer.getShort();
        sup2.attribExtensionNames = buffer.getStringList(true);
        sup2.extensionNames = buffer.getStringList(true);
        return sup2;
    }

    /**
     */
    public static class Supported2 {
        public int supportedAttributeMask;
        public int supportedAttributeBits;
        public int supportedOpenFlags;
        public int supportedAccessMask;
        public int maxReadSize;
        public short supportedOpenBlockVector;
        public short supportedBlock;
        //        uint32 attrib-extension-count
        public Collection<String> attribExtensionNames;
        //        uint32 extension-count
        public Collection<String> extensionNames;

        @Override
        public String toString() {
            return "attrsMask=0x" + Integer.toHexString(supportedAttributeMask)
                    + ",attrsBits=0x" + Integer.toHexString(supportedAttributeBits)
                    + ",openFlags=0x" + Integer.toHexString(supportedOpenFlags)
                    + ",accessMask=0x" + Integer.toHexString(supportedAccessMask)
                    + ",maxRead=" + maxReadSize
                    + ",openBlock=0x" + Integer.toHexString(supportedOpenBlockVector & 0xFFFF)
                    + ",block=" + Integer.toHexString(supportedBlock & 0xFFFF)
                    + ",attribs=" + attribExtensionNames
                    + ",exts=" + extensionNames;
        }
    }
}

package org.xbib.io.sshd.common.subsystem.sftp.extensions;

import org.xbib.io.sshd.common.subsystem.sftp.SftpConstants;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;

/**
 *
 */
public class VendorIdParser extends AbstractParser<VendorIdParser.VendorId> {
    public static final VendorIdParser INSTANCE = new VendorIdParser();

    public VendorIdParser() {
        super(SftpConstants.EXT_VENDOR_ID);
    }

    @Override
    public VendorId parse(byte[] input, int offset, int len) {
        return parse(new ByteArrayBuffer(input, offset, len));
    }

    public VendorId parse(Buffer buffer) {
        VendorId id = new VendorId();
        id.vendorName = buffer.getString();
        id.productName = buffer.getString();
        id.productVersion = buffer.getString();
        id.productBuildNumber = buffer.getLong();
        return id;
    }

    /**
     * The &quot;vendor-id&quot; information as per
     * <A HREF="http://tools.ietf.org/wg/secsh/draft-ietf-secsh-filexfer/draft-ietf-secsh-filexfer-09.txt">DRAFT 09 - section 4.4</A>
     */
    public static class VendorId {
        public String vendorName;
        public String productName;
        public String productVersion;
        public long productBuildNumber;

        @Override
        public String toString() {
            return vendorName + "-" + productName + "-" + productVersion + "-" + productBuildNumber;
        }
    }

}

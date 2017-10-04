package org.xbib.io.sshd.common.subsystem.sftp.extensions;

import org.xbib.io.sshd.common.subsystem.sftp.SftpConstants;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.buffer.BufferUtils;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 *
 */
public class NewlineParser extends AbstractParser<NewlineParser.Newline> {
    public static final NewlineParser INSTANCE = new NewlineParser();

    public NewlineParser() {
        super(SftpConstants.EXT_NEWLINE);
    }

    @Override
    public Newline parse(byte[] input, int offset, int len) {
        return parse(new String(input, offset, len, StandardCharsets.UTF_8));
    }

    public Newline parse(String value) {
        return new Newline(value);
    }

    /**
     * The &quot;newline&quot; extension information as per
     * <A HREF="http://tools.ietf.org/wg/secsh/draft-ietf-secsh-filexfer/draft-ietf-secsh-filexfer-09.txt">DRAFT 09 Section 4.3</A>
     */
    public static class Newline implements Cloneable, Serializable {
        private static final long serialVersionUID = 2010656704254497899L;
        private String newline;

        public Newline() {
            this(null);
        }

        public Newline(String newline) {
            this.newline = newline;
        }

        public String getNewline() {
            return newline;
        }

        public void setNewline(String newline) {
            this.newline = newline;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(getNewline());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }

            return Objects.equals(((Newline) obj).getNewline(), getNewline());
        }

        @Override
        public Newline clone() {
            try {
                return getClass().cast(super.clone());
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException("Failed to clone " + toString() + ": " + e.getMessage(), e);
            }
        }

        @Override
        public String toString() {
            String nl = getNewline();
            if (GenericUtils.isEmpty(nl)) {
                return nl;
            } else {
                return BufferUtils.toHex(':', nl.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}

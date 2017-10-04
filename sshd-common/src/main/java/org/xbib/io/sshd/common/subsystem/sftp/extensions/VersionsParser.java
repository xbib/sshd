package org.xbib.io.sshd.common.subsystem.sftp.extensions;

import org.xbib.io.sshd.common.subsystem.sftp.SftpConstants;
import org.xbib.io.sshd.common.util.GenericUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class VersionsParser extends AbstractParser<VersionsParser.Versions> {
    public static final VersionsParser INSTANCE = new VersionsParser();

    public VersionsParser() {
        super(SftpConstants.EXT_VERSIONS);
    }

    @Override
    public Versions parse(byte[] input, int offset, int len) {
        return parse(new String(input, offset, len, StandardCharsets.UTF_8));
    }

    public Versions parse(String value) {
        String[] comps = GenericUtils.split(value, Versions.SEP);
        return new Versions(GenericUtils.isEmpty(comps) ? Collections.emptyList() : Arrays.asList(comps));
    }

    /**
     * The &quot;versions&quot; extension data as per
     * <A HREF="http://tools.ietf.org/wg/secsh/draft-ietf-secsh-filexfer/draft-ietf-secsh-filexfer-09.txt">DRAFT 09 Section 4.6</A>
     */
    public static class Versions {
        public static final char SEP = ',';

        private List<String> versions;

        public Versions() {
            this(null);
        }

        public Versions(List<String> versions) {
            this.versions = versions;
        }

        public List<String> getVersions() {
            return versions;
        }

        public void setVersions(List<String> versions) {
            this.versions = versions;
        }

        @Override
        public String toString() {
            return GenericUtils.join(getVersions(), ',');
        }
    }
}

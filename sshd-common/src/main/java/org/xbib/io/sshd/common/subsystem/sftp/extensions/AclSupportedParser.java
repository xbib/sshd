package org.xbib.io.sshd.common.subsystem.sftp.extensions;

import org.xbib.io.sshd.common.subsystem.sftp.SftpConstants;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;
import org.xbib.io.sshd.common.util.logging.LoggingUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 */
public class AclSupportedParser extends AbstractParser<AclSupportedParser.AclCapabilities> {
    public static final AclSupportedParser INSTANCE = new AclSupportedParser();

    public AclSupportedParser() {
        super(SftpConstants.EXT_ACL_SUPPORTED);
    }

    @Override
    public AclCapabilities parse(byte[] input, int offset, int len) {
        return parse(new ByteArrayBuffer(input, offset, len));
    }

    public AclCapabilities parse(Buffer buffer) {
        return new AclCapabilities(buffer.getInt());
    }

    /**
     * The &quot;acl-supported&quot; information as per
     * <A HREF="https://tools.ietf.org/html/draft-ietf-secsh-filexfer-11">DRAFT 11 - section 5.4</A>
     */
    public static class AclCapabilities implements Serializable, Cloneable {
        private static final long serialVersionUID = -3118426327336468237L;
        private int capabilities;

        public AclCapabilities() {
            this(0);
        }

        public AclCapabilities(int capabilities) {
            this.capabilities = capabilities;
        }

        @SuppressWarnings("synthetic-access")
        public static Map<String, Integer> getAclCapabilityNamesMap() {
            return LazyAclCapabilityNameHolder.ACL_NAMES_MAP;
        }

        /**
         * @param name The ACL capability name - may be without the &quot;SSH_ACL_CAP_xxx&quot; prefix.
         *             Ignored if {@code null}/empty
         * @return The matching {@link Integer} value - or {@code null} if no match found
         */
        public static Integer getAclCapabilityValue(String name) {
            if (GenericUtils.isEmpty(name)) {
                return null;
            }

            name = name.toUpperCase();
            if (!name.startsWith(LazyAclCapabilityNameHolder.ACL_CAP_NAME_PREFIX)) {
                name += LazyAclCapabilityNameHolder.ACL_CAP_NAME_PREFIX;
            }

            Map<String, Integer> map = getAclCapabilityNamesMap();
            return map.get(name);
        }

        @SuppressWarnings("synthetic-access")
        public static Map<Integer, String> getAclCapabilityValuesMap() {
            return LazyAclCapabilityNameHolder.ACL_VALUES_MAP;
        }

        public static String getAclCapabilityName(int aclCapValue) {
            Map<Integer, String> map = getAclCapabilityValuesMap();
            String name = map.get(aclCapValue);
            if (GenericUtils.isEmpty(name)) {
                return Integer.toString(aclCapValue);
            } else {
                return name;
            }
        }

        public static Set<String> decodeAclCapabilities(int mask) {
            if (mask == 0) {
                return Collections.emptySet();
            }

            Set<String> caps = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            Map<Integer, String> map = getAclCapabilityValuesMap();
            map.forEach((value, name) -> {
                if ((mask & value) != 0) {
                    caps.add(name);
                }
            });

            return caps;
        }

        public static int constructAclCapabilities(Collection<Integer> maskValues) {
            if (GenericUtils.isEmpty(maskValues)) {
                return 0;
            }

            int mask = 0;
            for (Integer v : maskValues) {
                mask |= v;
            }

            return mask;
        }

        public static Set<Integer> deconstructAclCapabilities(int mask) {
            if (mask == 0) {
                return Collections.emptySet();
            }

            Map<Integer, String> map = getAclCapabilityValuesMap();
            Set<Integer> caps = new HashSet<>(map.size());
            for (Integer v : map.keySet()) {
                if ((mask & v) != 0) {
                    caps.add(v);
                }
            }

            return caps;
        }

        public int getCapabilities() {
            return capabilities;
        }

        public void setCapabilities(int capabilities) {
            this.capabilities = capabilities;
        }

        @Override
        public int hashCode() {
            return getCapabilities();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }

            return ((AclCapabilities) obj).getCapabilities() == getCapabilities();
        }

        @Override
        public AclCapabilities clone() {
            try {
                return getClass().cast(super.clone());
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException("Failed to clone " + toString() + ": " + e.getMessage(), e);
            }
        }

        @Override
        public String toString() {
            return Objects.toString(decodeAclCapabilities(getCapabilities()));
        }

        private static class LazyAclCapabilityNameHolder {
            private static final String ACL_CAP_NAME_PREFIX = "SSH_ACL_CAP_";
            private static final Map<Integer, String> ACL_VALUES_MAP = LoggingUtils.generateMnemonicMap(SftpConstants.class, ACL_CAP_NAME_PREFIX);
            private static final Map<String, Integer> ACL_NAMES_MAP =
                    Collections.unmodifiableMap(GenericUtils.flipMap(ACL_VALUES_MAP, GenericUtils.caseInsensitiveMap(), false));
        }
    }
}

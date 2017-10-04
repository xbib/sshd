package org.xbib.io.sshd.common.subsystem.sftp.extensions.openssh;

import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.subsystem.sftp.extensions.AbstractParser;
import org.xbib.io.sshd.common.util.ValidateUtils;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Base class for various {@code XXX@openssh.com} extension data reports.
 */
public abstract class AbstractOpenSSHExtensionParser extends AbstractParser<AbstractOpenSSHExtensionParser.OpenSSHExtension> {
    protected AbstractOpenSSHExtensionParser(String name) {
        super(name);
    }

    @Override
    public OpenSSHExtension parse(byte[] input, int offset, int len) {
        return parse(new String(input, offset, len, StandardCharsets.UTF_8));
    }

    public OpenSSHExtension parse(String version) {
        return new OpenSSHExtension(getName(), version);
    }

    public static class OpenSSHExtension implements NamedResource, Cloneable, Serializable {
        private static final long serialVersionUID = 5902797870154506909L;
        private final String name;
        private String version;

        public OpenSSHExtension(String name) {
            this(name, null);
        }

        public OpenSSHExtension(String name, String version) {
            this.name = ValidateUtils.checkNotNullAndNotEmpty(name, "No extension name");
            this.version = version;
        }

        @Override
        public final String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getName(), getVersion());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (this == obj) {
                return true;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }

            OpenSSHExtension other = (OpenSSHExtension) obj;
            return Objects.equals(getName(), other.getName())
                    && Objects.equals(getVersion(), other.getVersion());
        }

        @Override
        public OpenSSHExtension clone() {
            try {
                return getClass().cast(super.clone());
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException("Unexpected clone exception " + toString() + ": " + e.getMessage());
            }
        }

        @Override
        public String toString() {
            return getName() + " " + getVersion();
        }
    }
}

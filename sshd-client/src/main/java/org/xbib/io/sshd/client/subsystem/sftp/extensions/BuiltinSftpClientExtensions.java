package org.xbib.io.sshd.client.subsystem.sftp.extensions;

import org.xbib.io.sshd.client.subsystem.sftp.RawSftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.helpers.CheckFileHandleExtensionImpl;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.helpers.CheckFileNameExtensionImpl;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.helpers.CopyDataExtensionImpl;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.helpers.CopyFileExtensionImpl;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.helpers.MD5FileExtensionImpl;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.helpers.MD5HandleExtensionImpl;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.helpers.SpaceAvailableExtensionImpl;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.openssh.OpenSSHFsyncExtension;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.openssh.OpenSSHStatHandleExtension;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.openssh.OpenSSHStatPathExtension;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.openssh.helpers.OpenSSHFsyncExtensionImpl;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.openssh.helpers.OpenSSHStatHandleExtensionImpl;
import org.xbib.io.sshd.client.subsystem.sftp.extensions.openssh.helpers.OpenSSHStatPathExtensionImpl;
import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.subsystem.sftp.SftpConstants;
import org.xbib.io.sshd.common.subsystem.sftp.extensions.ParserUtils;
import org.xbib.io.sshd.common.subsystem.sftp.extensions.openssh.FstatVfsExtensionParser;
import org.xbib.io.sshd.common.subsystem.sftp.extensions.openssh.FsyncExtensionParser;
import org.xbib.io.sshd.common.subsystem.sftp.extensions.openssh.StatVfsExtensionParser;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 */
public enum BuiltinSftpClientExtensions implements SftpClientExtensionFactory {
    COPY_FILE(SftpConstants.EXT_COPY_FILE, CopyFileExtension.class) {
        @Override   // co-variant return
        public CopyFileExtension create(SftpClient client, RawSftpClient raw, Map<String, byte[]> extensions, Map<String, ?> parsed) {
            return new CopyFileExtensionImpl(client, raw, ParserUtils.supportedExtensions(parsed));
        }
    },
    COPY_DATA(SftpConstants.EXT_COPY_DATA, CopyDataExtension.class) {
        @Override   // co-variant return
        public CopyDataExtension create(SftpClient client, RawSftpClient raw, Map<String, byte[]> extensions, Map<String, ?> parsed) {
            return new CopyDataExtensionImpl(client, raw, ParserUtils.supportedExtensions(parsed));
        }
    },
    MD5_FILE(SftpConstants.EXT_MD5_HASH, MD5FileExtension.class) {
        @Override   // co-variant return
        public MD5FileExtension create(SftpClient client, RawSftpClient raw, Map<String, byte[]> extensions, Map<String, ?> parsed) {
            return new MD5FileExtensionImpl(client, raw, ParserUtils.supportedExtensions(parsed));
        }
    },
    MD5_HANDLE(SftpConstants.EXT_MD5_HASH_HANDLE, MD5HandleExtension.class) {
        @Override   // co-variant return
        public MD5HandleExtension create(SftpClient client, RawSftpClient raw, Map<String, byte[]> extensions, Map<String, ?> parsed) {
            return new MD5HandleExtensionImpl(client, raw, ParserUtils.supportedExtensions(parsed));
        }
    },
    CHECK_FILE_NAME(SftpConstants.EXT_CHECK_FILE_NAME, CheckFileNameExtension.class) {
        @Override   // co-variant return
        public CheckFileNameExtension create(SftpClient client, RawSftpClient raw, Map<String, byte[]> extensions, Map<String, ?> parsed) {
            return new CheckFileNameExtensionImpl(client, raw, ParserUtils.supportedExtensions(parsed));
        }
    },
    CHECK_FILE_HANDLE(SftpConstants.EXT_CHECK_FILE_HANDLE, CheckFileHandleExtension.class) {
        @Override   // co-variant return
        public CheckFileHandleExtension create(SftpClient client, RawSftpClient raw, Map<String, byte[]> extensions, Map<String, ?> parsed) {
            return new CheckFileHandleExtensionImpl(client, raw, ParserUtils.supportedExtensions(parsed));
        }
    },
    SPACE_AVAILABLE(SftpConstants.EXT_SPACE_AVAILABLE, SpaceAvailableExtension.class) {
        @Override   // co-variant return
        public SpaceAvailableExtension create(SftpClient client, RawSftpClient raw, Map<String, byte[]> extensions, Map<String, ?> parsed) {
            return new SpaceAvailableExtensionImpl(client, raw, ParserUtils.supportedExtensions(parsed));
        }
    },
    OPENSSH_FSYNC(FsyncExtensionParser.NAME, OpenSSHFsyncExtension.class) {
        @Override   // co-variant return
        public OpenSSHFsyncExtension create(SftpClient client, RawSftpClient raw, Map<String, byte[]> extensions, Map<String, ?> parsed) {
            return new OpenSSHFsyncExtensionImpl(client, raw, extensions);
        }
    },
    OPENSSH_STAT_HANDLE(FstatVfsExtensionParser.NAME, OpenSSHStatHandleExtension.class) {
        @Override   // co-variant return
        public OpenSSHStatHandleExtension create(SftpClient client, RawSftpClient raw, Map<String, byte[]> extensions, Map<String, ?> parsed) {
            return new OpenSSHStatHandleExtensionImpl(client, raw, extensions);
        }
    },
    OPENSSH_STAT_PATH(StatVfsExtensionParser.NAME, OpenSSHStatPathExtension.class) {
        @Override   // co-variant return
        public OpenSSHStatPathExtension create(SftpClient client, RawSftpClient raw, Map<String, byte[]> extensions, Map<String, ?> parsed) {
            return new OpenSSHStatPathExtensionImpl(client, raw, extensions);
        }
    };

    public static final Set<BuiltinSftpClientExtensions> VALUES =
            Collections.unmodifiableSet(EnumSet.allOf(BuiltinSftpClientExtensions.class));

    private final String name;

    private final Class<? extends SftpClientExtension> type;

    BuiltinSftpClientExtensions(String name, Class<? extends SftpClientExtension> type) {
        this.name = name;
        this.type = type;
    }

    public static BuiltinSftpClientExtensions fromName(String n) {
        return NamedResource.findByName(n, String.CASE_INSENSITIVE_ORDER, VALUES);
    }

    public static BuiltinSftpClientExtensions fromInstance(Object o) {
        return fromType((o == null) ? null : o.getClass());
    }

    public static BuiltinSftpClientExtensions fromType(Class<?> type) {
        if ((type == null) || (!SftpClientExtension.class.isAssignableFrom(type))) {
            return null;
        }

        // the base class is assignable to everybody so we cannot distinguish between the enum(s)
        if (SftpClientExtension.class == type) {
            return null;
        }

        for (BuiltinSftpClientExtensions v : VALUES) {
            Class<?> vt = v.getType();
            if (vt.isAssignableFrom(type)) {
                return v;
            }
        }

        return null;
    }

    @Override
    public final String getName() {
        return name;
    }

    public final Class<? extends SftpClientExtension> getType() {
        return type;
    }
}

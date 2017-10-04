package org.xbib.io.sshd.fs;

import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.common.util.GenericUtils;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;

/**
 *
 */
public class SftpPosixFileAttributeView extends AbstractSftpFileAttributeView implements PosixFileAttributeView {
    public SftpPosixFileAttributeView(SftpFileSystemProvider provider, Path path, LinkOption... options) {
        super(provider, path, options);
    }

    @Override
    public String name() {
        return "posix";
    }

    @Override
    public PosixFileAttributes readAttributes() throws IOException {
        return new SftpPosixFileAttributes(path, readRemoteAttributes());
    }

    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
        SftpClient.Attributes attrs = new SftpClient.Attributes();
        if (lastModifiedTime != null) {
            attrs.modifyTime(lastModifiedTime);
        }
        if (lastAccessTime != null) {
            attrs.accessTime(lastAccessTime);
        }
        if (createTime != null) {
            attrs.createTime(createTime);
        }

        if (GenericUtils.isEmpty(attrs.getFlags())) {
        } else {
            writeRemoteAttributes(attrs);
        }
    }

    @Override
    public void setPermissions(Set<PosixFilePermission> perms) throws IOException {
        provider.setAttribute(path, "permissions", perms, options);
    }

    @Override
    public void setGroup(GroupPrincipal group) throws IOException {
        provider.setAttribute(path, "group", group, options);
    }

    @Override
    public UserPrincipal getOwner() throws IOException {
        return readAttributes().owner();
    }

    @Override
    public void setOwner(UserPrincipal owner) throws IOException {
        provider.setAttribute(path, "owner", owner, options);
    }
}

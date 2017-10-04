package org.xbib.io.sshd.fs;

import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;
import org.xbib.io.sshd.common.util.GenericUtils;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;

/**
 *
 */
public class SftpPosixFileAttributes implements PosixFileAttributes {
    private final Path path;
    private final SftpClient.Attributes attributes;

    public SftpPosixFileAttributes(Path path, SftpClient.Attributes attributes) {
        this.path = path;
        this.attributes = attributes;
    }

    /**
     * @return The referenced attributes file {@link Path}
     */
    public final Path getPath() {
        return path;
    }

    @Override
    public UserPrincipal owner() {
        String owner = attributes.getOwner();
        return GenericUtils.isEmpty(owner) ? null : new SftpFileSystem.DefaultUserPrincipal(owner);
    }

    @Override
    public GroupPrincipal group() {
        String group = attributes.getGroup();
        return GenericUtils.isEmpty(group) ? null : new SftpFileSystem.DefaultGroupPrincipal(group);
    }

    @Override
    public Set<PosixFilePermission> permissions() {
        return SftpFileSystemProvider.permissionsToAttributes(attributes.getPermissions());
    }

    @Override
    public FileTime lastModifiedTime() {
        return attributes.getModifyTime();
    }

    @Override
    public FileTime lastAccessTime() {
        return attributes.getAccessTime();
    }

    @Override
    public FileTime creationTime() {
        return attributes.getCreateTime();
    }

    @Override
    public boolean isRegularFile() {
        return attributes.isRegularFile();
    }

    @Override
    public boolean isDirectory() {
        return attributes.isDirectory();
    }

    @Override
    public boolean isSymbolicLink() {
        return attributes.isSymbolicLink();
    }

    @Override
    public boolean isOther() {
        return attributes.isOther();
    }

    @Override
    public long size() {
        return attributes.getSize();
    }

    @Override
    public Object fileKey() {
        // TODO consider implementing this
        return null;
    }
}

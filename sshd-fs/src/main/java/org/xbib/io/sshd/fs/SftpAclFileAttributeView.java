package org.xbib.io.sshd.fs;

import org.xbib.io.sshd.client.subsystem.sftp.SftpClient;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.UserPrincipal;
import java.util.List;

/**
 *
 */
public class SftpAclFileAttributeView extends AbstractSftpFileAttributeView implements AclFileAttributeView {
    public SftpAclFileAttributeView(SftpFileSystemProvider provider, Path path, LinkOption... options) {
        super(provider, path, options);
    }

    @Override
    public UserPrincipal getOwner() throws IOException {
        PosixFileAttributes v = provider.readAttributes(path, PosixFileAttributes.class, options);
        return v.owner();
    }

    @Override
    public void setOwner(UserPrincipal owner) throws IOException {
        provider.setAttribute(path, "posix", "owner", owner, options);
    }

    @Override
    public String name() {
        return "acl";
    }

    @Override
    public List<AclEntry> getAcl() throws IOException {
        return readRemoteAttributes().getAcl();
    }

    @Override
    public void setAcl(List<AclEntry> acl) throws IOException {
        writeRemoteAttributes(new SftpClient.Attributes().acl(acl));
    }

}

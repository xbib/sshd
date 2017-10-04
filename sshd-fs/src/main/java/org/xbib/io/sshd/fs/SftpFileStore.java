package org.xbib.io.sshd.fs;

import org.xbib.io.sshd.common.subsystem.sftp.SftpConstants;
import org.xbib.io.sshd.common.util.GenericUtils;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.Collection;

/**
 *
 */
public class SftpFileStore extends FileStore {
    private final SftpFileSystem fs;
    private final String name;

    public SftpFileStore(String name, SftpFileSystem fs) {
        this.name = name;
        this.fs = fs;
    }

    public final SftpFileSystem getFileSystem() {
        return fs;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String type() {
        return SftpConstants.SFTP_SUBSYSTEM_NAME;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public long getTotalSpace() throws IOException {
        return Long.MAX_VALUE;  // TODO use SFTPv6 space-available extension
    }

    @Override
    public long getUsableSpace() throws IOException {
        return Long.MAX_VALUE;
    }

    @Override
    public long getUnallocatedSpace() throws IOException {
        return Long.MAX_VALUE;
    }

    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        SftpFileSystem sftpFs = getFileSystem();
        SftpFileSystemProvider provider = sftpFs.provider();
        return provider.isSupportedFileAttributeView(sftpFs, type);
    }

    @Override
    public boolean supportsFileAttributeView(String name) {
        if (GenericUtils.isEmpty(name)) {
            return false;   // debug breakpoint
        }

        FileSystem sftpFs = getFileSystem();
        Collection<String> views = sftpFs.supportedFileAttributeViews();
        return !GenericUtils.isEmpty(views) && views.contains(name);
    }

    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
        return null;    // no special views supported
    }

    @Override
    public Object getAttribute(String attribute) throws IOException {
        return null;    // no special attributes supported
    }
}

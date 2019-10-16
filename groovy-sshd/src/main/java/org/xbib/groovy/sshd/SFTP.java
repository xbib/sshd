package org.xbib.groovy.sshd;

import groovy.lang.Closure;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SFTP {

    private static final Logger logger = Logger.getLogger(SFTP.class.getName());

    private static final int READ_BUFFER_SIZE = 128 * 1024;

    private static final int WRITE_BUFFER_SIZE = 128 * 1024;

    private static final Set<PosixFilePermission> DEFAULT_DIR_PERMISSIONS =
            PosixFilePermissions.fromString("rwxr-xr-x");

    private static final Set<PosixFilePermission> DEFAULT_FILE_PERMISSIONS =
            PosixFilePermissions.fromString("rw-r--r--");

    private final String url;

    private final Map<String, ?> env;

    private SFTP(String url, Map<String, ?> env) {
        this.url = url;
        this.env = env;
    }

    public static SFTP newInstance() {
        return newInstance("sftp://localhost:22");
    }

    public static SFTP newInstance(Map<String, ?> env) {
        return newInstance("sftp://localhost:22", env);
    }

    public static SFTP newInstance(String url) {
        return newInstance(url, Collections.emptyMap());
    }

    public static SFTP newInstance(String url, Map<String, ?> env) {
        return new SFTP(url, env);
    }

    public Boolean exists(String path) throws Exception {
        return performWithContext(ctx -> Files.exists(ctx.fileSystem.getPath(path)));
    }

    public Boolean isExecutable(String path) throws Exception {
        return performWithContext(ctx -> Files.isExecutable(ctx.fileSystem.getPath(path)));
    }

    public Boolean isDirectory(String path) throws Exception {
        return performWithContext(ctx -> Files.isDirectory(ctx.fileSystem.getPath(path)));
    }

    public Boolean isRegularFile(String path) throws Exception {
        return performWithContext(ctx -> Files.isRegularFile(ctx.fileSystem.getPath(path)));
    }

    public Boolean isHidden(String path) throws Exception {
        return performWithContext(ctx -> Files.isHidden(ctx.fileSystem.getPath(path)));
    }

    public Boolean isSameFile(String path1, String path2) throws Exception {
        return performWithContext(ctx -> Files.isSameFile(ctx.fileSystem.getPath(path1), ctx.fileSystem.getPath(path2)));
    }

    public Boolean isSymbolicLink(String path) throws Exception {
        return performWithContext(ctx -> Files.isSymbolicLink(ctx.fileSystem.getPath(path)));
    }

    public Boolean isReadable(String path) throws Exception {
        return performWithContext(ctx -> Files.isReadable(ctx.fileSystem.getPath(path)));
    }

    public Boolean isWritable(String path) throws Exception {
        return performWithContext(ctx -> Files.isWritable(ctx.fileSystem.getPath(path)));
    }

    public void createFile(String path, FileAttribute<?>... attributes) throws Exception {
        performWithContext(ctx -> Files.createFile(ctx.fileSystem.getPath(path), attributes));
    }

    public void createDirectory(String path, FileAttribute<?>... attributes) throws Exception {
        performWithContext(ctx -> Files.createDirectory(ctx.fileSystem.getPath(path), attributes));
    }

    public void createDirectories(String path, FileAttribute<?>... attributes) throws Exception {
        performWithContext(ctx -> Files.createDirectories(ctx.fileSystem.getPath(path), attributes));
    }

    public void setAttribute(String path, String attribute, Object value) throws Exception {
        performWithContext(ctx -> Files.setAttribute(ctx.fileSystem.getPath(path), attribute, value));
    }

    public void setPermissions(String path, Set<PosixFilePermission> permissions) throws Exception {
        performWithContext(ctx -> Files.setPosixFilePermissions(ctx.fileSystem.getPath(path), permissions));
    }

    public void setLastModifiedTime(String path, FileTime fileTime) throws Exception {
        performWithContext(ctx -> Files.setLastModifiedTime(ctx.fileSystem.getPath(path), fileTime));
    }
    
    public void setOwner(String path, UserPrincipal userPrincipal) throws Exception {
        performWithContext(ctx -> Files.setOwner(ctx.fileSystem.getPath(path), userPrincipal));
    }
    
    public void each(String path, Closure<?> closure) throws Exception {
        WithContext<Object> action = ctx -> {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(ctx.fileSystem.getPath(path))) {
                stream.forEach(closure::call);
            }
            return null;
        };
        performWithContext(action);
    }

    public void eachFilter(String path, DirectoryStream.Filter<Path> filter, Closure<?> closure) throws Exception {
        WithContext<Object> action = ctx -> {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(ctx.fileSystem.getPath(path), filter)) {
                stream.forEach(closure::call);
            }
            return null;
        };
        performWithContext(action);
    }

    public void upload(Path source, Path target, CopyOption... copyOptions) throws Exception {
        upload(source, target, DEFAULT_DIR_PERMISSIONS, DEFAULT_FILE_PERMISSIONS, copyOptions);
    }

    public void upload(Path source, Path target,
                       Set<PosixFilePermission> dirPermissions,
                       Set<PosixFilePermission> filePermissions,
                       CopyOption... copyOptions) throws Exception {
        WithContext<Object> action = ctx -> {
            upload(ctx, Files.newByteChannel(source), target, WRITE_BUFFER_SIZE,
                    dirPermissions, filePermissions, copyOptions);
            return null;
        };
        performWithContext(action);
    }

    public void upload(Path source, String target, CopyOption... copyOptions) throws Exception {
        upload(source, target, DEFAULT_DIR_PERMISSIONS, DEFAULT_FILE_PERMISSIONS, copyOptions);
    }

    public void upload(Path source, String target,
                       Set<PosixFilePermission> dirPermissions,
                       Set<PosixFilePermission> filePermissions,
                       CopyOption... copyOptions) throws Exception {
        WithContext<Object> action = ctx -> {
            upload(ctx, Files.newByteChannel(source), ctx.fileSystem.getPath(target), WRITE_BUFFER_SIZE,
                    dirPermissions, filePermissions, copyOptions);
            return null;
        };
        performWithContext(action);
    }

    public void upload(InputStream source, Path target, CopyOption... copyOptions) throws Exception {
        upload(source, target, DEFAULT_DIR_PERMISSIONS, DEFAULT_FILE_PERMISSIONS, copyOptions);
    }

    public void upload(InputStream source, Path target,
                       Set<PosixFilePermission> dirPermissions,
                       Set<PosixFilePermission> filePermissions,
                       CopyOption... copyOptions) throws Exception {
        WithContext<Object> action = ctx -> {
            upload(ctx, Channels.newChannel(source), target, WRITE_BUFFER_SIZE,
                    dirPermissions, filePermissions, copyOptions);
            return null;
        };
        performWithContext(action);
    }

    public void upload(InputStream source, String target, CopyOption... copyOptions) throws Exception {
        upload(source, target, DEFAULT_DIR_PERMISSIONS, DEFAULT_FILE_PERMISSIONS, copyOptions);
    }

    public void upload(InputStream source, String target,
                       Set<PosixFilePermission> dirPermissions,
                       Set<PosixFilePermission> filePermissions,
                       CopyOption... copyOptions) throws Exception {
        WithContext<Object> action = ctx -> {
            upload(ctx, Channels.newChannel(source), ctx.fileSystem.getPath(target), WRITE_BUFFER_SIZE,
                    dirPermissions, filePermissions, copyOptions);
            return null;
        };
        performWithContext(action);
    }

    public void download(Path source, Path target,
                         CopyOption... copyOptions) throws Exception {
        download(source, target, DEFAULT_DIR_PERMISSIONS, DEFAULT_FILE_PERMISSIONS, copyOptions);
    }
    
    public void download(Path source, Path target,
                         Set<PosixFilePermission> dirPermissions,
                         Set<PosixFilePermission> filePermissions,
                         CopyOption... copyOptions) throws Exception {
        WithContext<Object> action = ctx -> {
            download(ctx, source, target, READ_BUFFER_SIZE,
                    dirPermissions, filePermissions, copyOptions);
            return null;
        };
        performWithContext(action);
    }

    public void download(String source, Path target,
                         CopyOption... copyOptions) throws Exception {
        download(source, target, DEFAULT_DIR_PERMISSIONS, DEFAULT_FILE_PERMISSIONS, copyOptions);
    }

    public void download(String source, Path target,
                         Set<PosixFilePermission> dirPermissions,
                         Set<PosixFilePermission> filePermissions,
                         CopyOption... copyOptions) throws Exception {
        WithContext<Object> action = ctx -> {
            download(ctx, ctx.fileSystem.getPath(source), target, READ_BUFFER_SIZE,
                    dirPermissions, filePermissions, copyOptions);
            return null;
        };
        performWithContext(action);
    }

    public void download(Path source, OutputStream target) throws Exception {
        WithContext<Object> action = ctx -> {
            download(ctx, source, target, READ_BUFFER_SIZE);
            return null;
        };
        performWithContext(action);
    }

    public void download(String source, OutputStream target) throws Exception {
        WithContext<Object> action = ctx -> {
            download(ctx, ctx.fileSystem.getPath(source), target, READ_BUFFER_SIZE);
            return null;
        };
        performWithContext(action);
    }

    public void rename(String source, String target, CopyOption... copyOptions) throws Exception {
        WithContext<Object> action = ctx -> {
            Files.move(ctx.fileSystem.getPath(source), ctx.fileSystem.getPath(target), copyOptions);
            return null;
        };
        performWithContext(action);
    }

    public void remove(String source) throws Exception {
        WithContext<Object> action = ctx -> {
            Files.deleteIfExists(ctx.fileSystem.getPath(source));
            return null;
        };
        performWithContext(action);
    }

    private void upload(SFTPContext ctx,
                        ReadableByteChannel source,
                        Path target,
                        int bufferSize,
                        Set<PosixFilePermission> dirPerms,
                        Set<PosixFilePermission> filePerms,
                        CopyOption... copyOptions) throws Exception {
        prepareForWrite(target, dirPerms, filePerms);
        transfer(source, ctx.provider.newByteChannel(target, prepareWriteOptions(copyOptions)), bufferSize);
    }

    private void download(SFTPContext ctx,
                          Path source,
                          OutputStream outputStream,
                          int bufferSize) throws Exception {
        download(ctx, source, Channels.newChannel(outputStream), bufferSize);
    }

    private void download(SFTPContext ctx,
                          Path source,
                          WritableByteChannel writableByteChannel,
                          int bufferSize) throws Exception {
        transfer(ctx.provider.newByteChannel(source, prepareReadOptions()), writableByteChannel,
                bufferSize);
    }

    private void download(SFTPContext ctx,
                          Path source,
                          Path target,
                          int bufferSize,
                          Set<PosixFilePermission> dirPerms,
                          Set<PosixFilePermission> filePerms,
                          CopyOption... copyOptions) throws Exception {
        prepareForWrite(target, dirPerms, filePerms);
        transfer(ctx.provider.newByteChannel(source, prepareReadOptions(copyOptions)),
                Files.newByteChannel(target, prepareWriteOptions(copyOptions)), bufferSize);
    }

    private void prepareForWrite(Path path,
                                 Set<PosixFilePermission> dirPerms,
                                 Set<PosixFilePermission> filePerms) throws IOException {
        try {
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent, PosixFilePermissions.asFileAttribute(dirPerms));
            }
            if (!Files.exists(path)) {
                Files.createFile(path, PosixFilePermissions.asFileAttribute(filePerms));
            }
            PosixFileAttributeView posixFileAttributeView =
                    Files.getFileAttributeView(path, PosixFileAttributeView.class);
            posixFileAttributeView.setPermissions(filePerms);
        } catch (FileAlreadyExistsException e) {
            logger.log(Level.SEVERE, "should not happen: file already exists: " + path);
        }
    }

    private Set<? extends OpenOption> prepareReadOptions(CopyOption... copyOptions) {
        // ignore user copy options
        return EnumSet.of(StandardOpenOption.READ);
    }

    private Set<? extends OpenOption> prepareWriteOptions(CopyOption... copyOptions) {
        Set<? extends OpenOption> options = null;
        for (CopyOption copyOption : copyOptions) {
            if (copyOption == StandardCopyOption.REPLACE_EXISTING) {
                options = EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        }
        if (options == null) {
            // we can not use CREATE_NEW, file is already there because of prepareForWrite() -> Files.createFile()
            options = EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }
        return options;
    }

    private void transfer(ReadableByteChannel readableByteChannel,
                          WritableByteChannel writableByteChannel,
                          int bufferSize) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        int read;
        while ((read = readableByteChannel.read(buffer)) > 0) {
            buffer.flip();
            while (read > 0) {
                read -= writableByteChannel.write(buffer);
            }
            buffer.clear();
        }
    }

    private <T> T performWithContext(WithContext<T> action) throws Exception {
        SFTPContext ctx = null;
        try {
            if (url != null) {
                ctx = new SFTPContext(URI.create(url), env);
                return action.perform(ctx);
            } else {
                return null;
            }
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }
}

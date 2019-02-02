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
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 */
public class SFTP {

    private static final Logger logger = Logger.getLogger(SFTP.class.getName());

    private static final int READ_BUFFER_SIZE = 128 * 1024;

    private static final int WRITE_BUFFER_SIZE = 128 * 1024;

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
        WithContext<Object> action = ctx -> {
            upload(ctx, Files.newByteChannel(source), target, WRITE_BUFFER_SIZE, copyOptions);
            return null;
        };
        performWithContext(action);
    }

    public void upload(Path source, String target, CopyOption... copyOptions) throws Exception {
        WithContext<Object> action = ctx -> {
            upload(ctx, Files.newByteChannel(source), ctx.fileSystem.getPath(target), WRITE_BUFFER_SIZE, copyOptions);
            return null;
        };
        performWithContext(action);
    }

    public void upload(InputStream source, Path target, CopyOption... copyOptions) throws Exception {
        WithContext<Object> action = ctx -> {
            upload(ctx, Channels.newChannel(source), target, WRITE_BUFFER_SIZE, copyOptions);
            return null;
        };
        performWithContext(action);
    }

    public void upload(InputStream source, String target, CopyOption... copyOptions) throws Exception {
        WithContext<Object> action = ctx -> {
            upload(ctx, Channels.newChannel(source), ctx.fileSystem.getPath(target), WRITE_BUFFER_SIZE, copyOptions);
            return null;
        };
        performWithContext(action);
    }

    public void download(Path source, Path target, CopyOption... copyOptions) throws Exception {
        WithContext<Object> action = ctx -> {
            download(ctx, source, target, READ_BUFFER_SIZE, copyOptions);
            return null;
        };
        performWithContext(action);
    }

    public void download(String source, Path target, CopyOption... copyOptions) throws Exception {
        WithContext<Object> action = ctx -> {
            download(ctx, ctx.fileSystem.getPath(source), target, READ_BUFFER_SIZE, copyOptions);
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

    public void rename(String source, String target) throws Exception {
        WithContext<Object> action = ctx -> {
            Files.move(ctx.fileSystem.getPath(source), ctx.fileSystem.getPath(target));
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
                        InputStream source,
                        Path target,
                        int bufferSize,
                        CopyOption... copyOptions) throws Exception {
        upload(ctx, Channels.newChannel(source), target, bufferSize, copyOptions);
    }

    private void upload(SFTPContext ctx,
                        Path source,
                        Path target,
                        int bufferSize,
                        CopyOption... copyOptions) throws Exception {
        upload(ctx, Files.newByteChannel(source, READ), target, bufferSize, copyOptions);
    }

    private void upload(SFTPContext ctx,
                        ReadableByteChannel source,
                        Path target,
                        int bufferSize,
                        CopyOption... copyOptions) throws Exception {
        preparePath(target);
        transfer(source, ctx.provider.newByteChannel(target, prepareWriteOptions(copyOptions), prepareFileAttributes()), bufferSize);
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
        transfer(ctx.provider.newByteChannel(source, EnumSet.of(READ)), writableByteChannel, bufferSize);
    }

    private void download(SFTPContext ctx,
                          Path source,
                          Path target,
                          int bufferSize,
                          CopyOption... copyOptions) throws Exception {
        preparePath(target);
        transfer(ctx.provider.newByteChannel(source, EnumSet.of(READ)),
                Files.newByteChannel(target, prepareWriteOptions(copyOptions), prepareFileAttributes()), bufferSize);
    }

    private void preparePath(Path path) throws IOException {
        try {
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
        } catch (FileAlreadyExistsException e) {
            logger.log(Level.SEVERE, "parent already exists as file: " + path);
        }
    }

    private  FileAttribute<Set<PosixFilePermission>> prepareFileAttributes() {
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-r--r--");
        return PosixFilePermissions.asFileAttribute(perms);
    }

    private Set<StandardOpenOption> prepareWriteOptions(CopyOption... copyOptions) {
        Set<StandardOpenOption> options = null;
        for (CopyOption copyOption : copyOptions) {
            if (copyOption == StandardCopyOption.REPLACE_EXISTING) {
                options = EnumSet.of(CREATE, WRITE);
            }
        }
        if (options == null) {
            options = EnumSet.of(CREATE_NEW, WRITE);
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

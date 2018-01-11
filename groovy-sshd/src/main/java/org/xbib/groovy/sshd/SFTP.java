package org.xbib.groovy.sshd;

import groovy.lang.Closure;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public class SFTP {

    private static final Logger logger = Logger.getLogger(SFTP.class.getName());

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
        return newInstance(url, null);
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
            try {
                Path parent = target.getParent();
                if (!Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
            } catch (FileAlreadyExistsException e) {
                logger.log(Level.SEVERE, "parent already exists as file: " + target);

            }
            Files.copy(source, target, copyOptions);
            return null;
        };
        performWithContext(action);
    }

    public void upload(Path source, String targetName, CopyOption... copyOptions) throws Exception {
        WithContext<Object> action = ctx -> {
            Path target = ctx.fileSystem.getPath(targetName);
            try {
                Path parent = target.getParent();
                if (!Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
            } catch (FileAlreadyExistsException e) {
                logger.log(Level.SEVERE, "parent already exists as file: " + target);
            }
            Files.copy(source, target, copyOptions);
            return null;
        };
        performWithContext(action);
    }

    public void upload(InputStream source, String targetName, CopyOption... copyOptions) throws Exception {
        WithContext<Object> action = ctx -> {
            Path target = ctx.fileSystem.getPath(targetName);
            try {
                Path parent = target.getParent();
                if (!Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
            } catch (FileAlreadyExistsException e) {
                logger.log(Level.SEVERE, "parent already exists as file: " + target);
            }
            Files.copy(source, target, copyOptions);
            return null;
        };
        performWithContext(action);
    }

    public void download(Path source, Path target, CopyOption... copyOptions) throws Exception {
        WithContext<Object> action = ctx -> {
            Files.copy(source, target, copyOptions);
            return null;
        };
        performWithContext(action);
    }

    public void download(String source, Path target, CopyOption... copyOptions) throws Exception {
        WithContext<Object> action = ctx -> {
            try {
                Path parent = target.getParent();
                if (!Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
            } catch (FileAlreadyExistsException e) {
                logger.log(Level.SEVERE, "parent already exists as file: " + target);
            }
            Files.copy(ctx.fileSystem.getPath(source), target, copyOptions);
            return null;
        };
        performWithContext(action);
    }

    public void download(String source, OutputStream target) throws Exception {
        WithContext<Object> action = ctx -> {
            Files.copy(ctx.fileSystem.getPath(source), target);
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

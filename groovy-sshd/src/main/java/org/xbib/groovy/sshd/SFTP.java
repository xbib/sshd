package org.xbib.groovy.sshd;

import groovy.lang.Closure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 */
public class SFTP {

    private static final Logger logger = LogManager.getLogger(SFTP.class.getName());

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

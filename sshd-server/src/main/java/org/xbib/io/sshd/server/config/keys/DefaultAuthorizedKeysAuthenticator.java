package org.xbib.io.sshd.server.config.keys;

import org.xbib.io.sshd.common.auth.UsernameHolder;
import org.xbib.io.sshd.common.config.keys.AuthorizedKeyEntry;
import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.util.OsUtils;
import org.xbib.io.sshd.common.util.Pair;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.io.IoUtils;
import org.xbib.io.sshd.server.session.ServerSession;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Monitors the {@code ~/.ssh/authorized_keys} file of the user currently running
 * the server, re-loading it if necessary. It also (optionally) enforces the same
 * permissions regime as {@code OpenSSH} does for the file permissions. By default
 * also compares the current username with the authenticated one.
 */
public class DefaultAuthorizedKeysAuthenticator extends AuthorizedKeysAuthenticator implements UsernameHolder {

    /**
     * The default instance that enforces the same permissions regime as {@code OpenSSH}
     */
    public static final DefaultAuthorizedKeysAuthenticator INSTANCE = new DefaultAuthorizedKeysAuthenticator(true);

    private final boolean strict;
    private final String user;

    /**
     * @param strict If {@code true} then makes sure that the containing folder
     *               has 0700 access and the file 0600. <B>Note:</B> for <I>Windows</I> it
     *               does not check these permissions
     */
    public DefaultAuthorizedKeysAuthenticator(boolean strict) {
        this(OsUtils.getCurrentUser(), strict);
    }

    public DefaultAuthorizedKeysAuthenticator(String user, boolean strict) {
        this(user, getDefaultAuthorizedKeysFile(), strict);
    }

    public DefaultAuthorizedKeysAuthenticator(File file, boolean strict) {
        this(Objects.requireNonNull(file, "No file provided").toPath(), strict, IoUtils.getLinkOptions(true));
    }

    public DefaultAuthorizedKeysAuthenticator(String user, File file, boolean strict) {
        this(user, Objects.requireNonNull(file, "No file provided").toPath(), strict, IoUtils.getLinkOptions(true));
    }

    public DefaultAuthorizedKeysAuthenticator(Path path, boolean strict, LinkOption... options) {
        this(OsUtils.getCurrentUser(), path, strict, options);
    }

    public DefaultAuthorizedKeysAuthenticator(String user, Path path, boolean strict, LinkOption... options) {
        super(path, options);
        this.user = ValidateUtils.checkNotNullAndNotEmpty(user, "No username provided");
        this.strict = strict;
    }

    @Override
    public final String getUsername() {
        return user;
    }

    public final boolean isStrict() {
        return strict;
    }

    @Override
    protected boolean isValidUsername(String username, ServerSession session) {
        if (!super.isValidUsername(username, session)) {
            return false;
        }

        String expected = getUsername();
        return username.equals(expected);
    }

    @Override
    protected Collection<AuthorizedKeyEntry> reloadAuthorizedKeys(Path path, String username, ServerSession session) throws IOException {
        if (isStrict()) {
            Pair<String, Object> violation = KeyUtils.validateStrictKeyFilePermissions(path);
            if (violation != null) {
                updateReloadAttributes();
                return Collections.emptyList();
            }
        }

        return super.reloadAuthorizedKeys(path, username, session);
    }

    /**
     * @param path     The {@link Path} to be validated
     * @param perms    The current {@link PosixFilePermission}s
     * @param excluded The permissions <U>not</U> allowed to exist
     * @return The original path
     * @throws IOException If an excluded permission appears in the current ones
     */
    protected Path validateFilePath(Path path, Collection<PosixFilePermission> perms, Collection<PosixFilePermission> excluded) throws IOException {
        PosixFilePermission p = IoUtils.validateExcludedPermissions(perms, excluded);
        if (p != null) {
            String filePath = path.toString();
            throw new FileSystemException(filePath, filePath, "File not allowed to have " + p + " permission: " + filePath);
        }

        return path;
    }
}
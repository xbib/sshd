package org.xbib.io.sshd.common.util.io;

import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.OsUtils;
import org.xbib.io.sshd.common.util.Pair;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Watches over changes for a file and re-loads them if file has changed - including
 * if file is deleted or (re-)created.
 */
public class ModifiableFileWatcher extends AbstractLoggingBean {

    /**
     * The {@link Set} of {@link PosixFilePermission} <U>not</U> allowed if strict
     * permissions are enforced on key files
     */
    public static final Set<PosixFilePermission> STRICTLY_PROHIBITED_FILE_PERMISSION =
            Collections.unmodifiableSet(
                    EnumSet.of(PosixFilePermission.GROUP_WRITE, PosixFilePermission.OTHERS_WRITE));

    protected final LinkOption[] options;

    private final Path file;
    private final AtomicBoolean lastExisted = new AtomicBoolean(false);
    private final AtomicLong lastSize = new AtomicLong(Long.MIN_VALUE);
    private final AtomicLong lastModified = new AtomicLong(-1L);

    public ModifiableFileWatcher(File file) {
        this(Objects.requireNonNull(file, "No file to watch").toPath());
    }

    public ModifiableFileWatcher(Path file) {
        this(file, org.xbib.io.sshd.common.util.io.IoUtils.getLinkOptions(true));
    }

    public ModifiableFileWatcher(Path file, LinkOption... options) {
        this.file = Objects.requireNonNull(file, "No path to watch");
        // use a clone to avoid being sensitive to changes in the passed array
        this.options = (options == null) ? org.xbib.io.sshd.common.util.io.IoUtils.EMPTY_LINK_OPTIONS : options.clone();
    }

    /**
     * <P>Checks if a path has strict permissions</P>
     * <UL>
     * <LI><P>
     * (For {@code Unix}) The path may not have group or others write permissions
     * </P></LI>
     * <LI><P>
     * The path must be owned by current user.
     * </P></LI>
     * <LI><P>
     * (For {@code Unix}) The path may be owned by root.
     * </P></LI>
     * </UL>
     *
     * @param path    The {@link Path} to be checked - ignored if {@code null}
     *                or does not exist
     * @param options The {@link LinkOption}s to use to query the file's permissions
     * @return The violated permission as {@link Pair} where {@link Pair#getClass()}
     * is a loggable message and {@link Pair#getSecond()} is the offending object
     * - e.g., {@link PosixFilePermission} or {@link String} for owner. Return
     * value is {@code null} if no violations detected
     * @throws IOException If failed to retrieve the permissions
     * @see #STRICTLY_PROHIBITED_FILE_PERMISSION
     */
    public static Pair<String, Object> validateStrictConfigFilePermissions(Path path, LinkOption... options) throws IOException {
        if ((path == null) || (!Files.exists(path, options))) {
            return null;
        }

        Collection<PosixFilePermission> perms = org.xbib.io.sshd.common.util.io.IoUtils.getPermissions(path, options);
        if (GenericUtils.isEmpty(perms)) {
            return null;
        }

        if (OsUtils.isUNIX()) {
            PosixFilePermission p = org.xbib.io.sshd.common.util.io.IoUtils.validateExcludedPermissions(perms, STRICTLY_PROHIBITED_FILE_PERMISSION);
            if (p != null) {
                return new Pair<>(String.format("Permissions violation (%s)", p), p);
            }
        }

        String owner = IoUtils.getFileOwner(path, options);
        if (GenericUtils.isEmpty(owner)) {
            // we cannot get owner
            // general issue: jvm does not support permissions
            // security issue: specific filesystem does not support permissions
            return null;
        }

        String current = OsUtils.getCurrentUser();
        Set<String> expected = new HashSet<>();
        expected.add(current);
        if (OsUtils.isUNIX()) {
            // Windows "Administrator" was considered however in Windows most likely a group is used.
            expected.add(OsUtils.ROOT_USER);
        }

        if (!expected.contains(owner)) {
            return new Pair<>(String.format("Owner violation (%s)", owner), owner);
        }

        return null;
    }

    /**
     * @return The watched {@link Path}
     */
    public final Path getPath() {
        return file;
    }

    public final boolean exists() throws IOException {
        return Files.exists(getPath(), options);
    }

    public final long size() throws IOException {
        if (exists()) {
            return Files.size(getPath());
        } else {
            return -1L;
        }
    }

    public final FileTime lastModified() throws IOException {
        if (exists()) {
            BasicFileAttributes attrs = Files.readAttributes(getPath(), BasicFileAttributes.class, options);
            return attrs.lastModifiedTime();
        } else {
            return null;
        }
    }

    /**
     * @return {@code true} if the watched file has probably been changed
     * @throws IOException If failed to query file data
     */
    public boolean checkReloadRequired() throws IOException {
        boolean exists = exists();
        // if existence state changed from last time
        if (exists != lastExisted.getAndSet(exists)) {
            return true;
        }

        if (!exists) {
            // file did not exist and still does not exist
            resetReloadAttributes();
            return false;
        }

        long size = size();
        if (size < 0L) {
            // means file no longer exists
            resetReloadAttributes();
            return true;
        }

        // if size changed then obviously need reload
        if (size != lastSize.getAndSet(size)) {
            return true;
        }

        FileTime modifiedTime = lastModified();
        if (modifiedTime == null) {
            // means file no longer exists
            resetReloadAttributes();
            return true;
        }

        long timestamp = modifiedTime.toMillis();
        return timestamp != lastModified.getAndSet(timestamp);

    }

    /**
     * Resets the state attributes used to detect changes to the initial
     * construction values - i.e., file assumed not to exist and no known
     * size of modify time
     */
    public void resetReloadAttributes() {
        lastExisted.set(false);
        lastSize.set(Long.MIN_VALUE);
        lastModified.set(-1L);
    }

    /**
     * May be called to refresh the state attributes used to detect changes
     * e.g., file existence, size and last-modified time once re-loading is
     * successfully completed. If the file does not exist then the attributes
     * are reset to an &quot;unknown&quot; state.
     *
     * @throws IOException If failed to access the file (if exists)
     * @see #resetReloadAttributes()
     */
    public void updateReloadAttributes() throws IOException {
        if (exists()) {
            long size = size();
            FileTime modifiedTime = lastModified();

            if ((size >= 0L) && (modifiedTime != null)) {
                lastExisted.set(true);
                lastSize.set(size);
                lastModified.set(modifiedTime.toMillis());
                return;
            }
        }

        resetReloadAttributes();
    }

    @Override
    public String toString() {
        return Objects.toString(getPath());
    }
}

package org.xbib.io.sshd.client.config.hosts;

import org.xbib.io.sshd.common.util.Pair;
import org.xbib.io.sshd.common.util.io.IoUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Monitors the {@code ~/.ssh/config} file of the user currently running
 * the client, re-loading it if necessary. It also (optionally) enforces
 * the same permissions regime as {@code OpenSSH}
 */
public class DefaultConfigFileHostEntryResolver extends ConfigFileHostEntryResolver {
    /**
     * The default instance that enforces the same permissions regime as {@code OpenSSH}
     */
    public static final DefaultConfigFileHostEntryResolver INSTANCE = new DefaultConfigFileHostEntryResolver(true);

    private final boolean strict;

    /**
     * @param strict If {@code true} then makes sure that the containing folder
     *               has 0700 access and the file 0644. <B>Note:</B> for <I>Windows</I> it
     *               does not check these permissions
     * @see #validateStrictConfigFilePermissions(Path, LinkOption...)
     */
    public DefaultConfigFileHostEntryResolver(boolean strict) {
        this(org.xbib.io.sshd.client.config.hosts.HostConfigEntry.getDefaultHostConfigFile(), strict);
    }

    public DefaultConfigFileHostEntryResolver(File file, boolean strict) {
        this(Objects.requireNonNull(file, "No file provided").toPath(), strict, IoUtils.getLinkOptions(true));
    }

    public DefaultConfigFileHostEntryResolver(Path path, boolean strict, LinkOption... options) {
        super(path, options);
        this.strict = strict;
    }

    /**
     * @return If {@code true} then makes sure that the containing folder
     * has 0700 access and the file 0644. <B>Note:</B> for <I>Windows</I> it
     * does not check these permissions
     * @see #validateStrictConfigFilePermissions(Path, LinkOption...)
     */
    public final boolean isStrict() {
        return strict;
    }

    @Override
    protected List<HostConfigEntry> reloadHostConfigEntries(Path path, String host, int port, String username) throws IOException {
        if (isStrict()) {
            Pair<String, Object> violation = validateStrictConfigFilePermissions(path);
            if (violation != null) {
                updateReloadAttributes();
                return Collections.emptyList();
            }
        }

        return super.reloadHostConfigEntries(path, host, port, username);
    }
}

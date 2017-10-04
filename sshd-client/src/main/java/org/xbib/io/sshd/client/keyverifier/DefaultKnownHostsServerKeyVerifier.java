package org.xbib.io.sshd.client.keyverifier;

import org.xbib.io.sshd.client.config.hosts.KnownHostEntry;
import org.xbib.io.sshd.common.util.Pair;
import org.xbib.io.sshd.common.util.io.IoUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Monitors the {@code ~/.ssh/known_hosts} file of the user currently running
 * the client, updating and re-loading it if necessary. It also (optionally)
 * enforces the same permissions regime as {@code OpenSSH}.
 */
public class DefaultKnownHostsServerKeyVerifier extends KnownHostsServerKeyVerifier {
    private final boolean strict;

    public DefaultKnownHostsServerKeyVerifier(ServerKeyVerifier delegate) {
        this(delegate, true);
    }

    public DefaultKnownHostsServerKeyVerifier(ServerKeyVerifier delegate, boolean strict) {
        this(delegate, strict, KnownHostEntry.getDefaultKnownHostsFile(), IoUtils.getLinkOptions(true));
    }

    public DefaultKnownHostsServerKeyVerifier(ServerKeyVerifier delegate, boolean strict, File file) {
        this(delegate, strict, Objects.requireNonNull(file, "No file provided").toPath(), IoUtils.getLinkOptions(true));
    }

    public DefaultKnownHostsServerKeyVerifier(ServerKeyVerifier delegate, boolean strict, Path file, LinkOption... options) {
        super(delegate, file, options);
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
    protected List<HostEntryPair> reloadKnownHosts(Path file) throws IOException, GeneralSecurityException {
        if (isStrict()) {
            Pair<String, Object> violation = validateStrictConfigFilePermissions(file);
            if (violation != null) {
                updateReloadAttributes();
                return Collections.emptyList();
            }
        }

        return super.reloadKnownHosts(file);
    }
}

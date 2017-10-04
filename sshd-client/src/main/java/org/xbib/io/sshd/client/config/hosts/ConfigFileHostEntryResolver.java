package org.xbib.io.sshd.client.config.hosts;

import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.io.IoUtils;
import org.xbib.io.sshd.common.util.io.ModifiableFileWatcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Watches for changes in a configuration file and automatically reloads any changes.
 */
public class ConfigFileHostEntryResolver extends ModifiableFileWatcher implements HostConfigEntryResolver {
    private final AtomicReference<HostConfigEntryResolver> delegateHolder = // assumes initially empty
            new AtomicReference<>(HostConfigEntryResolver.EMPTY);

    public ConfigFileHostEntryResolver(File file) {
        this(Objects.requireNonNull(file, "No file to watch").toPath());
    }

    public ConfigFileHostEntryResolver(Path file) {
        this(file, IoUtils.EMPTY_LINK_OPTIONS);
    }

    public ConfigFileHostEntryResolver(Path file, LinkOption... options) {
        super(file, options);
    }

    @Override
    public HostConfigEntry resolveEffectiveHost(String host, int port, String username) throws IOException {
        try {
            HostConfigEntryResolver delegate = Objects.requireNonNull(resolveEffectiveResolver(host, port, username), "No delegate");
            HostConfigEntry entry = delegate.resolveEffectiveHost(host, port, username);

            return entry;
        } catch (Throwable e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw new IOException(e);
            }
        }
    }

    protected HostConfigEntryResolver resolveEffectiveResolver(String host, int port, String username) throws IOException {
        if (checkReloadRequired()) {
            delegateHolder.set(HostConfigEntryResolver.EMPTY);  // start fresh

            Path path = getPath();
            if (exists()) {
                Collection<HostConfigEntry> entries = reloadHostConfigEntries(path, host, port, username);
                if (GenericUtils.size(entries) > 0) {
                    delegateHolder.set(HostConfigEntry.toHostConfigEntryResolver(entries));
                }
            } else {
            }
        }

        return delegateHolder.get();
    }

    protected List<HostConfigEntry> reloadHostConfigEntries(Path path, String host, int port, String username) throws IOException {
        List<HostConfigEntry> entries = HostConfigEntry.readHostConfigEntries(path);
        updateReloadAttributes();
        return entries;
    }
}

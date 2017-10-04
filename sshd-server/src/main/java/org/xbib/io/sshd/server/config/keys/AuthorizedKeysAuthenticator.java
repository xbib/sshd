package org.xbib.io.sshd.server.config.keys;

import org.xbib.io.sshd.common.config.keys.AuthorizedKeyEntry;
import org.xbib.io.sshd.common.config.keys.PublicKeyEntry;
import org.xbib.io.sshd.common.config.keys.PublicKeyEntryResolver;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.io.IoUtils;
import org.xbib.io.sshd.common.util.io.ModifiableFileWatcher;
import org.xbib.io.sshd.server.auth.pubkey.KeySetPublickeyAuthenticator;
import org.xbib.io.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.xbib.io.sshd.server.auth.pubkey.RejectAllPublickeyAuthenticator;
import org.xbib.io.sshd.server.session.ServerSession;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Uses the authorized keys file to implement {@link PublickeyAuthenticator}
 * while automatically re-loading the keys if the file has changed when a
 * new authentication request is received. <B>Note:</B> by default, the only
 * validation of the username is that it is not {@code null}/empty - see
 * {@link #isValidUsername(String, ServerSession)}
 */
public class AuthorizedKeysAuthenticator extends ModifiableFileWatcher implements PublickeyAuthenticator {

    /**
     * Standard OpenSSH authorized keys file name
     */
    public static final String STD_AUTHORIZED_KEYS_FILENAME = "authorized_keys";
    private final AtomicReference<PublickeyAuthenticator> delegateHolder =  // assumes initially reject-all
            new AtomicReference<>(RejectAllPublickeyAuthenticator.INSTANCE);

    public AuthorizedKeysAuthenticator(File file) {
        this(Objects.requireNonNull(file, "No file to watch").toPath());
    }

    public AuthorizedKeysAuthenticator(Path file) {
        this(file, IoUtils.EMPTY_LINK_OPTIONS);
    }

    public AuthorizedKeysAuthenticator(Path file, LinkOption... options) {
        super(file, options);
    }

    /**
     * @return The default {@link Path} location of the OpenSSH authorized keys file
     */
    @SuppressWarnings("synthetic-access")
    public static Path getDefaultAuthorizedKeysFile() {
        return LazyDefaultAuthorizedKeysFileHolder.KEYS_FILE;
    }

    /**
     * Reads read the contents of the default OpenSSH <code>authorized_keys</code> file
     *
     * @param options The {@link OpenOption}s to use when reading the file
     * @return A {@link List} of all the {@link AuthorizedKeyEntry}-ies found there -
     * or empty if file does not exist
     * @throws IOException If failed to read keys from file
     */
    public static List<AuthorizedKeyEntry> readDefaultAuthorizedKeys(OpenOption... options) throws IOException {
        Path keysFile = getDefaultAuthorizedKeysFile();
        if (Files.exists(keysFile, IoUtils.EMPTY_LINK_OPTIONS)) {
            return AuthorizedKeyEntry.readAuthorizedKeys(keysFile);
        } else {
            return Collections.emptyList();
        }
    }

    public static PublickeyAuthenticator fromAuthorizedEntries(PublicKeyEntryResolver fallbackResolver, Collection<? extends AuthorizedKeyEntry> entries)
            throws IOException, GeneralSecurityException {
        Collection<PublicKey> keys = resolveAuthorizedKeys(fallbackResolver, entries);
        if (GenericUtils.isEmpty(keys)) {
            return RejectAllPublickeyAuthenticator.INSTANCE;
        } else {
            return new KeySetPublickeyAuthenticator(keys);
        }
    }

    public static List<PublicKey> resolveAuthorizedKeys(PublicKeyEntryResolver fallbackResolver, Collection<? extends AuthorizedKeyEntry> entries)
            throws IOException, GeneralSecurityException {
        if (GenericUtils.isEmpty(entries)) {
            return Collections.emptyList();
        }

        List<PublicKey> keys = new ArrayList<>(entries.size());
        for (AuthorizedKeyEntry e : entries) {
            PublicKey k = e.resolvePublicKey(fallbackResolver);
            if (k != null) {
                keys.add(k);
            }
        }

        return keys;
    }

    @Override
    public boolean authenticate(String username, PublicKey key, ServerSession session) {
        if (!isValidUsername(username, session)) {
            return false;
        }

        try {
            PublickeyAuthenticator delegate =
                    Objects.requireNonNull(resolvePublickeyAuthenticator(username, session), "No delegate");
            boolean accepted = delegate.authenticate(username, key, session);
            return accepted;
        } catch (Throwable e) {
            return false;
        }
    }

    protected boolean isValidUsername(String username, ServerSession session) {
        return GenericUtils.isNotEmpty(username);
    }

    protected PublickeyAuthenticator resolvePublickeyAuthenticator(String username, ServerSession session)
            throws IOException, GeneralSecurityException {
        if (checkReloadRequired()) {
            /* Start fresh - NOTE: if there is any error then we want to reject all attempts
             * since we don't want to remain with the previous data - safer that way
             */
            delegateHolder.set(RejectAllPublickeyAuthenticator.INSTANCE);

            Path path = getPath();
            if (exists()) {
                Collection<AuthorizedKeyEntry> entries = reloadAuthorizedKeys(path, username, session);
                if (GenericUtils.size(entries) > 0) {
                    delegateHolder.set(fromAuthorizedEntries(getFallbackPublicKeyEntryResolver(), entries));
                }
            } else {
                log.info("resolvePublickeyAuthenticator(" + username + ")[" + session + "] no authorized keys file at " + path);
            }
        }

        return delegateHolder.get();
    }

    protected PublicKeyEntryResolver getFallbackPublicKeyEntryResolver() {
        return PublicKeyEntryResolver.IGNORING;
    }

    protected Collection<AuthorizedKeyEntry> reloadAuthorizedKeys(Path path, String username, ServerSession session) throws IOException {
        Collection<AuthorizedKeyEntry> entries = AuthorizedKeyEntry.readAuthorizedKeys(path);
        log.info("reloadAuthorizedKeys(" + username + ")[" + session + "] loaded " + GenericUtils.size(entries) + " keys from " + path);
        updateReloadAttributes();
        return entries;
    }

    private static final class LazyDefaultAuthorizedKeysFileHolder {
        private static final Path KEYS_FILE = PublicKeyEntry.getDefaultKeysFolderPath().resolve(STD_AUTHORIZED_KEYS_FILENAME);
    }

}

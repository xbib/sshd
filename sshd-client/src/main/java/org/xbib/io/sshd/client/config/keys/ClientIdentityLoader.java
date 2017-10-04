package org.xbib.io.sshd.client.config.keys;

import org.xbib.io.sshd.common.config.keys.FilePasswordProvider;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.io.IoUtils;
import org.xbib.io.sshd.common.util.security.SecurityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

/**
 */
public interface ClientIdentityLoader {
    /**
     * A default implementation that assumes a file location that <U>must</U> exist.
     * <B>Note:</B> It calls {@link SecurityUtils#loadKeyPairIdentity(String, InputStream, FilePasswordProvider)}
     */
    ClientIdentityLoader DEFAULT = new ClientIdentityLoader() {
        @Override
        public boolean isValidLocation(String location) throws IOException {
            Path path = toPath(location);
            return Files.exists(path, IoUtils.EMPTY_LINK_OPTIONS);
        }

        @Override
        public KeyPair loadClientIdentity(String location, FilePasswordProvider provider) throws IOException, GeneralSecurityException {
            Path path = toPath(location);
            try (InputStream inputStream = Files.newInputStream(path, IoUtils.EMPTY_OPEN_OPTIONS)) {
                return SecurityUtils.loadKeyPairIdentity(path.toString(), inputStream, provider);
            }
        }

        @Override
        public String toString() {
            return "DEFAULT";
        }

        private Path toPath(String location) {
            Path path = Paths.get(ValidateUtils.checkNotNullAndNotEmpty(location, "No location"));
            path = path.toAbsolutePath();
            path = path.normalize();
            return path;
        }
    };

    /**
     * @param location The identity key-pair location - the actual meaning (file, URL, etc.)
     *                 depends on the implementation.
     * @return {@code true} if it represents a valid location - the actual meaning of
     * the validity depends on the implementation
     * @throws IOException If failed to validate the location
     */
    boolean isValidLocation(String location) throws IOException;

    /**
     * @param location The identity key-pair location - the actual meaning (file, URL, etc.)
     *                 depends on the implementation.
     * @param provider The {@link FilePasswordProvider} to consult if the location contains
     *                 an encrypted identity
     * @return The loaded {@link KeyPair} - {@code null} if location is empty
     * and it is OK that it does not exist
     * @throws IOException              If failed to access / process the remote location
     * @throws GeneralSecurityException If failed to convert the contents into
     *                                  a valid identity
     */
    KeyPair loadClientIdentity(String location, FilePasswordProvider provider) throws IOException, GeneralSecurityException;
}

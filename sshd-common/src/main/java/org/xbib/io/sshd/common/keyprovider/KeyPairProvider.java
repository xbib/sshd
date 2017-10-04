package org.xbib.io.sshd.common.keyprovider;

import org.xbib.io.sshd.common.cipher.ECCurves;
import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;

import java.security.KeyPair;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Provider for key pairs.  This provider is used on the server side to provide
 * the host key, or on the client side to provide the user key.
 */
public interface KeyPairProvider extends KeyIdentityProvider {

    /**
     * SSH identifier for RSA keys
     */
    String SSH_RSA = "ssh-rsa";

    /**
     * SSH identifier for DSA keys
     */
    String SSH_DSS = "ssh-dss";

    /**
     * SSH identifier for ED25519 elliptic curve keys
     */
    String SSH_ED25519 = "ssh-ed25519";

    /**
     * SSH identifier for EC keys in NIST curve P-256
     */
    String ECDSA_SHA2_NISTP256 = ECCurves.nistp256.getKeyType();

    /**
     * SSH identifier for EC keys in NIST curve P-384
     */
    String ECDSA_SHA2_NISTP384 = ECCurves.nistp384.getKeyType();

    /**
     * SSH identifier for EC keys in NIST curve P-521
     */
    String ECDSA_SHA2_NISTP521 = ECCurves.nistp521.getKeyType();

    /**
     * A {@link KeyPairProvider} that has no keys
     */
    KeyPairProvider EMPTY_KEYPAIR_PROVIDER =
            new KeyPairProvider() {
                @Override
                public KeyPair loadKey(String type) {
                    return null;
                }

                @Override
                public Iterable<String> getKeyTypes() {
                    return Collections.emptyList();
                }

                @Override
                public Iterable<KeyPair> loadKeys() {
                    return Collections.emptyList();
                }

                @Override
                public String toString() {
                    return "EMPTY_KEYPAIR_PROVIDER";
                }
            };

    /**
     * Wrap the provided {@link KeyPair}s into a {@link KeyPairProvider}
     *
     * @param pairs The available pairs - ignored if {@code null}/empty (i.e.,
     *              returns {@link #EMPTY_KEYPAIR_PROVIDER})
     * @return The provider wrapper
     * @see #wrap(Iterable)
     */
    static KeyPairProvider wrap(KeyPair... pairs) {
        return GenericUtils.isEmpty(pairs) ? EMPTY_KEYPAIR_PROVIDER : wrap(Arrays.asList(pairs));
    }

    /**
     * Wrap the provided {@link KeyPair}s into a {@link KeyPairProvider}
     *
     * @param pairs The available pairs {@link Iterable} - ignored if {@code null} (i.e.,
     *              returns {@link #EMPTY_KEYPAIR_PROVIDER})
     * @return The provider wrapper
     */
    static KeyPairProvider wrap(Iterable<KeyPair> pairs) {
        return (pairs == null) ? EMPTY_KEYPAIR_PROVIDER : new KeyPairProvider() {
            @Override
            public Iterable<KeyPair> loadKeys() {
                return pairs;
            }

            @Override
            public KeyPair loadKey(String type) {
                for (KeyPair kp : pairs) {
                    String t = KeyUtils.getKeyType(kp);
                    if (Objects.equals(type, t)) {
                        return kp;
                    }
                }

                return null;
            }

            @Override
            public Iterable<String> getKeyTypes() {
                // use a LinkedHashSet so as to preserve the order but avoid duplicates
                Collection<String> types = new LinkedHashSet<>();
                for (KeyPair kp : pairs) {
                    String t = KeyUtils.getKeyType(kp);
                    if (GenericUtils.isEmpty(t)) {
                        continue;   // avoid unknown key types
                    }

                    if (!types.add(t)) {
                        continue;   // debug breakpoint
                    }
                }

                return types;
            }
        };
    }

    /**
     * Load a key of the specified type which can be &quot;ssh-rsa&quot;, &quot;ssh-dss&quot;,
     * or &quot;ecdsa-sha2-nistp{256,384,521}&quot;. If there is no key of this type, return
     * {@code null}
     *
     * @param type the type of key to load
     * @return a valid key pair or {@code null} if this type of key is not available
     */
    default KeyPair loadKey(String type) {
        ValidateUtils.checkNotNullAndNotEmpty(type, "No key type to load");
        return GenericUtils.stream(loadKeys())
                .filter(key -> type.equals(KeyUtils.getKeyType(key)))
                .findFirst()
                .orElse(null);
    }

    /**
     * @return The available {@link Iterable} key types in preferred order - never {@code null}
     */
    default Iterable<String> getKeyTypes() {
        return GenericUtils.stream(loadKeys())
                .map(KeyUtils::getKeyType)
                .filter(GenericUtils::isNotEmpty)
                .collect(Collectors.toSet());
    }
}

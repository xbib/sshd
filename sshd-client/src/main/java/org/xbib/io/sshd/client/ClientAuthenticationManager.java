package org.xbib.io.sshd.client;

import org.xbib.io.sshd.client.auth.AuthenticationIdentitiesProvider;
import org.xbib.io.sshd.client.auth.BuiltinUserAuthFactories;
import org.xbib.io.sshd.client.auth.UserAuth;
import org.xbib.io.sshd.client.auth.keyboard.UserInteraction;
import org.xbib.io.sshd.client.auth.password.PasswordIdentityProvider;
import org.xbib.io.sshd.client.keyverifier.ServerKeyVerifier;
import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.keyprovider.KeyPairProviderHolder;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;

import java.security.KeyPair;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Holds information required for the client to perform authentication with the server.
 */
public interface ClientAuthenticationManager extends KeyPairProviderHolder {

    /**
     * Ordered comma separated list of authentications methods.
     * Authentications methods accepted by the server will be tried in the given order.
     * If not configured or {@code null}/empty, then the session's {@link #getUserAuthFactories()}
     * is used as-is
     */
    String PREFERRED_AUTHS = "preferred-auths";

    /**
     * Specifies the number of interactive prompts before giving up.
     * The argument to this keyword must be an integer.
     *
     * @see #DEFAULT_PASSWORD_PROMPTS
     */
    String PASSWORD_PROMPTS = "password-prompts";

    /**
     * Default value for {@link #PASSWORD_PROMPTS} if none configured
     */
    int DEFAULT_PASSWORD_PROMPTS = 3;

    /**
     * @return The {@link AuthenticationIdentitiesProvider} to be used for attempting
     * password or public key authentication
     */
    AuthenticationIdentitiesProvider getRegisteredIdentities();

    /**
     * Retrieve {@link PasswordIdentityProvider} used to provide password
     * candidates
     *
     * @return The {@link PasswordIdentityProvider} instance - ignored if {@code null}
     * (i.e., no passwords available)
     */
    PasswordIdentityProvider getPasswordIdentityProvider();

    void setPasswordIdentityProvider(PasswordIdentityProvider provider);

    /**
     * @param password Password to be added - may not be {@code null}.
     *                 <B>Note:</B> this password is <U>in addition</U> to whatever passwords
     *                 are available via the {@link PasswordIdentityProvider} (if any)
     */
    void addPasswordIdentity(char[] password);

    /**
     * @param key The {@link KeyPair} to add - may not be {@code null}
     *            <B>Note:</B> this key is <U>in addition</U> to whatever keys
     *            are available via the {@link org.xbib.io.sshd.common.keyprovider.KeyIdentityProvider} (if any)
     */
    void addPublicKeyIdentity(KeyPair key);

    /**
     * @param kp The {@link KeyPair} to remove - ignored if {@code null}
     * @return The removed {@link KeyPair} - same one that was added via
     * {@link #addPublicKeyIdentity(KeyPair)} - or {@code null} if no
     * match found
     */
    KeyPair removePublicKeyIdentity(KeyPair kp);

    /**
     * Retrieve the server key verifier to be used to check the key when connecting
     * to an SSH server.
     *
     * @return the {@link ServerKeyVerifier} to use - never {@code null}
     */
    ServerKeyVerifier getServerKeyVerifier();

    void setServerKeyVerifier(ServerKeyVerifier serverKeyVerifier);

    /**
     * @return A {@link UserInteraction} object to communicate with the user
     * (may be {@code null} to indicate that no such communication is allowed)
     */
    UserInteraction getUserInteraction();

    void setUserInteraction(UserInteraction userInteraction);

    /**
     * @return a {@link List} of {@link UserAuth} {@link NamedFactory}-ies - never
     * {@code null}/empty
     */
    List<NamedFactory<UserAuth>> getUserAuthFactories();

    void setUserAuthFactories(List<NamedFactory<UserAuth>> userAuthFactories);

    default String getUserAuthFactoriesNameList() {
        return NamedResource.getNames(getUserAuthFactories());
    }

    default void setUserAuthFactoriesNameList(String names) {
        setUserAuthFactoriesNames(GenericUtils.split(names, ','));
    }

    default List<String> getUserAuthFactoriesNames() {
        return NamedResource.getNameList(getUserAuthFactories());
    }

    default void setUserAuthFactoriesNames(Collection<String> names) {
        BuiltinUserAuthFactories.ParseResult result = BuiltinUserAuthFactories.parseFactoriesList(names);
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<NamedFactory<UserAuth>> factories =
                (List) ValidateUtils.checkNotNullAndNotEmpty(result.getParsedFactories(), "No supported cipher factories: %s", names);
        Collection<String> unsupported = result.getUnsupportedFactories();
        ValidateUtils.checkTrue(GenericUtils.isEmpty(unsupported), "Unsupported cipher factories found: %s", unsupported);
        setUserAuthFactories(factories);
    }

    default void setUserAuthFactoriesNames(String... names) {
        setUserAuthFactoriesNames(GenericUtils.isEmpty((Object[]) names) ? Collections.emptyList() : Arrays.asList(names));
    }
}

package org.xbib.io.sshd.server.auth;

import org.xbib.io.sshd.common.Factory;
import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.config.NamedFactoriesListParseResult;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.server.auth.hostbased.UserAuthHostBasedFactory;
import org.xbib.io.sshd.server.auth.keyboard.UserAuthKeyboardInteractiveFactory;
import org.xbib.io.sshd.server.auth.password.UserAuthPasswordFactory;
import org.xbib.io.sshd.server.auth.pubkey.UserAuthPublicKeyFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 *
 */
public enum BuiltinUserAuthFactories implements NamedFactory<UserAuthFactory> {
    PASSWORD(UserAuthPasswordFactory.INSTANCE),
    PUBLICKEY(UserAuthPublicKeyFactory.INSTANCE),
    KBINTERACTIVE(UserAuthKeyboardInteractiveFactory.INSTANCE),
    HOSTBASED(UserAuthHostBasedFactory.INSTANCE);

    public static final Set<BuiltinUserAuthFactories> VALUES =
            Collections.unmodifiableSet(EnumSet.allOf(BuiltinUserAuthFactories.class));

    private final UserAuthFactory factory;

    BuiltinUserAuthFactories(UserAuthFactory factory) {
        this.factory = Objects.requireNonNull(factory, "No delegate factory instance");
    }

    /**
     * @param name The factory name (case <U>insensitive</U>) - ignored if {@code null}/empty
     * @return The matching factory instance - {@code null} if no match found
     */
    public static UserAuthFactory fromFactoryName(String name) {
        Factory<UserAuthFactory> factory = NamedResource.findByName(name, String.CASE_INSENSITIVE_ORDER, VALUES);
        if (factory == null) {
            return null;
        }

        return factory.create();
    }

    /**
     * @param factories A comma-separated list of factories' names - ignored if {@code null}/empty
     * @return A {@link ParseResult} containing the successfully parsed
     * factories and the unknown ones. <B>Note:</B> it is up to caller to
     * ensure that the lists do not contain duplicates
     */
    public static ParseResult parseFactoriesList(String factories) {
        return parseFactoriesList(GenericUtils.split(factories, ','));
    }

    public static ParseResult parseFactoriesList(String... factories) {
        return parseFactoriesList(GenericUtils.isEmpty((Object[]) factories) ? Collections.emptyList() : Arrays.asList(factories));
    }

    public static ParseResult parseFactoriesList(Collection<String> factories) {
        if (GenericUtils.isEmpty(factories)) {
            return ParseResult.EMPTY;
        }

        List<UserAuthFactory> resolved = new ArrayList<>(factories.size());
        List<String> unknown = Collections.emptyList();
        for (String name : factories) {
            UserAuthFactory c = resolveFactory(name);
            if (c != null) {
                resolved.add(c);
            } else {
                // replace the (unmodifiable) empty list with a real one
                if (unknown.isEmpty()) {
                    unknown = new ArrayList<>();
                }
                unknown.add(name);
            }
        }

        return new ParseResult(resolved, unknown);
    }

    public static UserAuthFactory resolveFactory(String name) {
        if (GenericUtils.isEmpty(name)) {
            return null;
        }

        return fromFactoryName(name);
    }

    @Override
    public UserAuthFactory create() {
        return factory;
    }

    @Override
    public String getName() {
        return factory.getName();
    }

    /**
     * Holds the result of {@link BuiltinUserAuthFactories#parseFactoriesList(String)}.
     */
    public static class ParseResult extends NamedFactoriesListParseResult<UserAuth, UserAuthFactory> {
        public static final ParseResult EMPTY = new ParseResult(Collections.emptyList(), Collections.emptyList());

        public ParseResult(List<UserAuthFactory> parsed, List<String> unsupported) {
            super(parsed, unsupported);
        }
    }
}

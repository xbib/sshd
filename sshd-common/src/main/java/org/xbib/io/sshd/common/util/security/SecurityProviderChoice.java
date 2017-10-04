package org.xbib.io.sshd.common.util.security;

import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.threads.ThreadUtils;

import java.security.Provider;
import java.util.Objects;

/**
 *
 */
public interface SecurityProviderChoice extends NamedResource {
    SecurityProviderChoice EMPTY = new SecurityProviderChoice() {
        @Override
        public String getName() {
            return null;
        }

        @Override
        public boolean isNamedProviderUsed() {
            return false;
        }

        @Override
        public Provider getSecurityProvider() {
            return null;
        }

        @Override
        public String toString() {
            return "EMPTY";
        }
    };

    static SecurityProviderChoice toSecurityProviderChoice(String name) {
        ValidateUtils.checkNotNullAndNotEmpty(name, "No name provided");
        return new SecurityProviderChoice() {
            private final String s = SecurityProviderChoice.class.getSimpleName() + "[" + name + "]";

            @Override
            public String getName() {
                return name;
            }

            @Override
            public boolean isNamedProviderUsed() {
                return true;
            }

            @Override
            public Provider getSecurityProvider() {
                return null;
            }

            @Override
            public String toString() {
                return s;
            }
        };
    }

    static SecurityProviderChoice toSecurityProviderChoice(Provider provider) {
        Objects.requireNonNull(provider, "No provider instance");
        return new SecurityProviderChoice() {
            private final String s = SecurityProviderChoice.class.getSimpleName()
                    + "[" + Provider.class.getSimpleName() + "]"
                    + "[" + provider.getName() + "]";

            @Override
            public String getName() {
                return provider.getName();
            }

            @Override
            public boolean isNamedProviderUsed() {
                return false;
            }

            @Override
            public Provider getSecurityProvider() {
                return provider;
            }

            @Override
            public String toString() {
                return s;
            }
        };
    }

    static Provider createProviderInstance(Class<?> anchor, String providerClassName)
            throws ReflectiveOperationException {
        return ThreadUtils.createDefaultInstance(anchor, Provider.class, providerClassName);
    }

    /**
     * @return {@code true} if to use the provider's name rather than its
     * {@link Provider} instance - default={@code true}.
     */
    default boolean isNamedProviderUsed() {
        return true;
    }

    /**
     * @return The security {@link Provider} to use in case {@link #isNamedProviderUsed()}
     * is {@code false}. Can be {@code null} if {@link #isNamedProviderUsed()} is {@code true},
     * but not recommended.
     */
    Provider getSecurityProvider();
}

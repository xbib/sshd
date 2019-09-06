package org.xbib.io.sshd.common.util.security;

import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

import java.security.Provider;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public abstract class AbstractSecurityProviderRegistrar
        extends AbstractLoggingBean
        implements SecurityProviderRegistrar {
    protected final Map<String, Object> props = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    protected final Map<Class<?>, Map<String, Boolean>> supportedEntities = new HashMap<>();
    protected final AtomicReference<Provider> providerHolder = new AtomicReference<>(null);

    private final String name;

    protected AbstractSecurityProviderRegistrar(String name) {
        this.name = ValidateUtils.checkNotNullAndNotEmpty(name, "No name provided");
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public Map<String, Object> getProperties() {
        return props;
    }

    @Override
    public boolean isSecurityEntitySupported(Class<?> entityType, String name) {
        Map<String, Boolean> supportMap;
        synchronized (supportedEntities) {
            supportMap = supportedEntities.computeIfAbsent(
                    entityType, k -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
        }

        Boolean supportFlag;
        synchronized (supportMap) {
            supportFlag = supportMap.computeIfAbsent(
                    name, k -> SecurityProviderRegistrar.super.isSecurityEntitySupported(entityType, name));
        }

        return supportFlag;
    }

    /**
     * Attempts to see if a provider with this name already registered. If not,
     * then uses reflection API in order to load and instantiate the specified
     * {@code providerClassName}
     *
     * @param providerClassName The fully-qualified class name to instantiate
     *                          if a provider not already registered
     * @return The resolved {@link Provider} instance - <B>Note:</B> the result
     * is <U>cached</U> - i.e., successful resolution result will not cause
     * the code to re-resolve the provider
     * @throws ReflectiveOperationException  If failed to instantiate the provider
     * @throws UnsupportedOperationException If registrar not supported
     * @see #isSupported()
     * @see Security#getProvider(String)
     * @see #createProviderInstance(String)
     */
    protected Provider getOrCreateProvider(String providerClassName) throws ReflectiveOperationException {
        if (!isSupported()) {
            throw new UnsupportedOperationException("Provider not supported");
        }

        Provider provider;
        boolean created = false;
        synchronized (providerHolder) {
            provider = providerHolder.get();
            if (provider != null) {
                return provider;
            }

            provider = Security.getProvider(getName());
            if (provider == null) {
                provider = createProviderInstance(providerClassName);
                created = true;
            }
            providerHolder.set(provider);
        }
        return provider;
    }

    protected Provider createProviderInstance(String providerClassName) throws ReflectiveOperationException {
        return SecurityProviderChoice.createProviderInstance(getClass(), providerClassName);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getName() + "]";
    }
}

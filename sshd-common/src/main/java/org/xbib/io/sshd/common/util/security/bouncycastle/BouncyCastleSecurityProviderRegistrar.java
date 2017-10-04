package org.xbib.io.sshd.common.util.security.bouncycastle;

import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ReflectionUtils;
import org.xbib.io.sshd.common.util.security.AbstractSecurityProviderRegistrar;
import org.xbib.io.sshd.common.util.security.SecurityUtils;
import org.xbib.io.sshd.common.util.threads.ThreadUtils;

import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.Signature;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class BouncyCastleSecurityProviderRegistrar extends AbstractSecurityProviderRegistrar {
    // We want to use reflection API so as not to require BouncyCastle to be present in the classpath
    public static final String PROVIDER_CLASS = "org.bouncycastle.jce.provider.BouncyCastleProvider";
    // Do not define a static registrar instance to minimize class loading issues
    private final AtomicReference<Boolean> supportHolder = new AtomicReference<>(null);
    private final AtomicReference<String> allSupportHolder = new AtomicReference<>();

    public BouncyCastleSecurityProviderRegistrar() {
        super(SecurityUtils.BOUNCY_CASTLE);
    }

    @Override
    public boolean isEnabled() {
        if (!super.isEnabled()) {
            return false;
        }

        // For backward compatibility
        return this.getBooleanProperty(SecurityUtils.REGISTER_BOUNCY_CASTLE_PROP, true);
    }

    @Override
    public Provider getSecurityProvider() {
        try {
            return getOrCreateProvider(PROVIDER_CLASS);
        } catch (ReflectiveOperationException t) {
            Throwable e = GenericUtils.peelException(t);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }

            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDefaultSecurityEntitySupportValue(Class<?> entityType) {
        String allValue = allSupportHolder.get();
        if (GenericUtils.length(allValue) > 0) {
            return allValue;
        }

        String propName = getConfigurationPropertyName("supportAll");
        allValue = this.getStringProperty(propName, ALL_OPTIONS_VALUE);
        if (GenericUtils.isEmpty(allValue)) {
            allValue = NO_OPTIONS_VALUE;
        }

        allSupportHolder.set(allValue);
        return allValue;
    }

    @Override
    public boolean isSecurityEntitySupported(Class<?> entityType, String name) {
        if (!isSupported()) {
            return false;
        }

        // Some known values it does not support
        if (KeyPairGenerator.class.isAssignableFrom(entityType)
                || KeyFactory.class.isAssignableFrom(entityType)) {
            if (Objects.compare(name, SecurityUtils.EDDSA, String.CASE_INSENSITIVE_ORDER) == 0) {
                return false;
            }
        } else if (Signature.class.isAssignableFrom(entityType)) {
            if (Objects.compare(name, SecurityUtils.CURVE_ED25519_SHA512, String.CASE_INSENSITIVE_ORDER) == 0) {
                return false;
            }
        }

        return super.isSecurityEntitySupported(entityType, name);
    }

    @Override
    public boolean isSupported() {
        Boolean supported;
        synchronized (supportHolder) {
            supported = supportHolder.get();
            if (supported != null) {
                return supported;
            }

            ClassLoader cl = ThreadUtils.resolveDefaultClassLoader(getClass());
            supported = ReflectionUtils.isClassAvailable(cl, PROVIDER_CLASS);
            supportHolder.set(supported);
        }

        return supported;
    }
}

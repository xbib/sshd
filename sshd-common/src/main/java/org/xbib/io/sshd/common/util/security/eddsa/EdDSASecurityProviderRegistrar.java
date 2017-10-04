package org.xbib.io.sshd.common.util.security.eddsa;

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
public class EdDSASecurityProviderRegistrar extends AbstractSecurityProviderRegistrar {

    public static final String PROVIDER_CLASS = "org.xbib.io.sshd.eddsa.EdDSASecurityProvider";

    // Do not define a static registrar instance to minimize class loading issues
    private final AtomicReference<Boolean> supportHolder = new AtomicReference<>(null);

    public EdDSASecurityProviderRegistrar() {
        super(SecurityUtils.EDDSA);
    }

    @Override
    public boolean isEnabled() {
        if (!super.isEnabled()) {
            return false;
        }
        // For backward compatibility
        return this.getBooleanProperty(SecurityUtils.EDDSA_SUPPORTED_PROP, true);
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
    public boolean isSecurityEntitySupported(Class<?> entityType, String name) {
        if (!isSupported()) {
            return false;
        }
        if (KeyPairGenerator.class.isAssignableFrom(entityType)
                || KeyFactory.class.isAssignableFrom(entityType)) {
            return Objects.compare(name, getName(), String.CASE_INSENSITIVE_ORDER) == 0;
        } else if (Signature.class.isAssignableFrom(entityType)) {
            return Objects.compare(SecurityUtils.CURVE_ED25519_SHA512, name, String.CASE_INSENSITIVE_ORDER) == 0;
        } else {
            return false;
        }
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
            supported = ReflectionUtils.isClassAvailable(cl, "net.i2p.crypto.eddsa.EdDSAKey");
            supportHolder.set(supported);
        }
        return supported;
    }
}

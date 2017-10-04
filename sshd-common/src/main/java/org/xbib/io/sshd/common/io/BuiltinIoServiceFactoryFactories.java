package org.xbib.io.sshd.common.io;

import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.io.nio2.Nio2ServiceFactoryFactory;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 *
 */
public enum BuiltinIoServiceFactoryFactories implements NamedFactory<IoServiceFactoryFactory> {
    NIO2(Nio2ServiceFactoryFactory.class);

    public static final Set<BuiltinIoServiceFactoryFactories> VALUES =
            Collections.unmodifiableSet(EnumSet.allOf(BuiltinIoServiceFactoryFactories.class));

    private final Class<? extends IoServiceFactoryFactory> factoryClass;

    BuiltinIoServiceFactoryFactories(Class<? extends IoServiceFactoryFactory> clazz) {
        factoryClass = clazz;
    }

    public static BuiltinIoServiceFactoryFactories fromFactoryName(String name) {
        return NamedResource.findByName(name, String.CASE_INSENSITIVE_ORDER, VALUES);
    }

    public static BuiltinIoServiceFactoryFactories fromFactoryClass(Class<?> clazz) {
        if ((clazz == null) || (!IoServiceFactoryFactory.class.isAssignableFrom(clazz))) {
            return null;
        }

        for (BuiltinIoServiceFactoryFactories f : VALUES) {
            if (clazz.isAssignableFrom(f.getFactoryClass())) {
                return f;
            }
        }

        return null;
    }

    public final Class<? extends IoServiceFactoryFactory> getFactoryClass() {
        return factoryClass;
    }

    @Override
    public final String getName() {
        return name().toLowerCase();
    }

    @Override
    public final IoServiceFactoryFactory create() {
        Class<? extends IoServiceFactoryFactory> clazz = getFactoryClass();
        try {
            return clazz.newInstance();
        } catch (Throwable e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }
}

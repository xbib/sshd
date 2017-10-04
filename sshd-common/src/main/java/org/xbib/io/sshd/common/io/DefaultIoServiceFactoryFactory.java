package org.xbib.io.sshd.common.io;

import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.threads.ExecutorServiceConfigurer;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;

/**
 *
 */
public class DefaultIoServiceFactoryFactory extends AbstractIoServiceFactoryFactory {

    private IoServiceFactoryFactory factory;

    public DefaultIoServiceFactoryFactory() {
        this(null, true);
    }

    protected DefaultIoServiceFactoryFactory(ExecutorService executors, boolean shutdownOnExit) {
        super(executors, shutdownOnExit);
    }

    public static <T extends IoServiceFactoryFactory> T newInstance(Class<T> clazz) {
        String factory = System.getProperty(clazz.getName());
        if (!GenericUtils.isEmpty(factory)) {
            return newInstance(clazz, factory);
        }

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            T t = tryLoad(ServiceLoader.load(clazz, cl));
            if (t != null) {
                return t;
            }
        }
        if (cl != DefaultIoServiceFactoryFactory.class.getClassLoader()) {
            T t = tryLoad(ServiceLoader.load(clazz, DefaultIoServiceFactoryFactory.class.getClassLoader()));
            if (t != null) {
                return t;
            }
        }
        throw new IllegalStateException("Could not find a valid sshd io provider");
    }

    public static <T extends IoServiceFactoryFactory> T tryLoad(ServiceLoader<T> loader) {
        Iterator<T> it = loader.iterator();
        try {
            while (it.hasNext()) {
                try {
                    return it.next();
                } catch (Throwable t) {
                }
            }
        } catch (Throwable t) {
        }
        return null;
    }

    public static <T extends IoServiceFactoryFactory> T newInstance(Class<T> clazz, String factory) {
        BuiltinIoServiceFactoryFactories builtin = BuiltinIoServiceFactoryFactories.fromFactoryName(factory);
        if (builtin != null) {
            return clazz.cast(builtin.create());
        }

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            try {
                return clazz.cast(cl.loadClass(factory).newInstance());
            } catch (Throwable t) {
            }
        }
        if (cl != DefaultIoServiceFactoryFactory.class.getClassLoader()) {
            try {
                return clazz.cast(DefaultIoServiceFactoryFactory.class.getClassLoader().loadClass(factory).newInstance());
            } catch (Throwable t) {
            }
        }
        throw new IllegalStateException("Unable to create instance of class " + factory);
    }

    @Override
    public IoServiceFactory create(FactoryManager manager) {
        return getFactory().create(manager);
    }

    private IoServiceFactoryFactory getFactory() {
        synchronized (this) {
            if (factory == null) {
                factory = newInstance(IoServiceFactoryFactory.class);
                if (factory instanceof ExecutorServiceConfigurer) {
                    ExecutorServiceConfigurer configurer = (ExecutorServiceConfigurer) factory;
                    configurer.setExecutorService(getExecutorService());
                    configurer.setShutdownOnExit(isShutdownOnExit());
                }
            }
        }
        return factory;
    }
}

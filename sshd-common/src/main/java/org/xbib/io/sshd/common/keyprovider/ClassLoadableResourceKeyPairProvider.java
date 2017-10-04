package org.xbib.io.sshd.common.keyprovider;

import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.threads.ThreadUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.security.KeyPair;
import java.util.Collection;
import java.util.Collections;

/**
 * This provider loads private keys from the specified resources that
 * are accessible via {@link ClassLoader#getResourceAsStream(String)}.
 * If no loader configured via {@link #setResourceLoader(ClassLoader)}, then
 * {@link ThreadUtils#resolveDefaultClassLoader(Class)} is used
 */
public class ClassLoadableResourceKeyPairProvider extends AbstractResourceKeyPairProvider<String> {
    private ClassLoader classLoader;
    private Collection<String> resources;

    public ClassLoadableResourceKeyPairProvider() {
        this(Collections.emptyList());
    }

    public ClassLoadableResourceKeyPairProvider(ClassLoader cl) {
        this(cl, Collections.emptyList());
    }

    public ClassLoadableResourceKeyPairProvider(String res) {
        this(Collections.singletonList(ValidateUtils.checkNotNullAndNotEmpty(res, "No resource specified")));
    }

    public ClassLoadableResourceKeyPairProvider(ClassLoader cl, String res) {
        this(cl, Collections.singletonList(ValidateUtils.checkNotNullAndNotEmpty(res, "No resource specified")));
    }

    public ClassLoadableResourceKeyPairProvider(Collection<String> resources) {
        this.classLoader = ThreadUtils.resolveDefaultClassLoader(getClass());
        this.resources = (resources == null) ? Collections.emptyList() : resources;
    }

    public ClassLoadableResourceKeyPairProvider(ClassLoader cl, Collection<String> resources) {
        this.classLoader = cl;
        this.resources = (resources == null) ? Collections.emptyList() : resources;
    }

    public Collection<String> getResources() {
        return resources;
    }

    public void setResources(Collection<String> resources) {
        this.resources = (resources == null) ? Collections.emptyList() : resources;
    }

    public ClassLoader getResourceLoader() {
        return classLoader;
    }

    public void setResourceLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Iterable<KeyPair> loadKeys() {
        return loadKeys(getResources());
    }

    @Override
    protected InputStream openKeyPairResource(String resourceKey, String resource) throws IOException {
        ClassLoader cl = resolveClassLoader();
        if (cl == null) {
            throw new StreamCorruptedException("No resource loader for " + resource);
        }

        InputStream input = cl.getResourceAsStream(resource);
        if (input == null) {
            throw new FileNotFoundException("Cannot find resource " + resource);
        }

        return input;
    }

    protected ClassLoader resolveClassLoader() {
        ClassLoader cl = getResourceLoader();
        if (cl == null) {
            cl = ThreadUtils.resolveDefaultClassLoader(getClass());
        }
        return cl;
    }
}

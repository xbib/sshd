package org.xbib.io.sshd.common.config;

import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.threads.ThreadUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 *
 */
public final class VersionProperties {
    private VersionProperties() {
        throw new UnsupportedOperationException("No instance");
    }

    public static Map<String, String> getVersionProperties() {
        return LazyHolder.PROPERTIES;
    }

    private static class LazyHolder {
        private static final Map<String, String> PROPERTIES =
                Collections.unmodifiableMap(loadVersionProperties(LazyHolder.class));

        private static Map<String, String> loadVersionProperties(Class<?> anchor) {
            return loadVersionProperties(anchor, ThreadUtils.resolveDefaultClassLoader(anchor));
        }

        private static Map<String, String> loadVersionProperties(Class<?> anchor, ClassLoader loader) {
            Map<String, String> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            try {
                InputStream input = loader.getResourceAsStream("org/xbib/io/sshd/sshd-version.properties");
                if (input == null) {
                    throw new FileNotFoundException("Resource does not exists");
                }

                Properties props = new Properties();
                try {
                    props.load(input);
                } finally {
                    input.close();
                }

                for (String key : props.stringPropertyNames()) {
                    String value = GenericUtils.trimToEmpty(props.getProperty(key));
                    if (GenericUtils.isEmpty(value)) {
                        continue;   // we have no need for empty value
                    }

                    String prev = result.put(key, value);
                    if (prev != null) {
                    }
                }
            } catch (Exception e) {
            }

            return result;
        }
    }

}

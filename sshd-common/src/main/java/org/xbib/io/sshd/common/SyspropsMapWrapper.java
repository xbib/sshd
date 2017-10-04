package org.xbib.io.sshd.common;

import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.Pair;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A wrapper that exposes a read-only {@link Map} access to the system
 * properties. Any attempt to modify it will throw {@link UnsupportedOperationException}.
 * The mapper uses the {@link #SYSPROPS_MAPPED_PREFIX} to filter and access'
 * only these properties, ignoring all others
 */
public final class SyspropsMapWrapper implements Map<String, Object> {
    /**
     * Prefix of properties used by the mapper to identify SSHD related settings
     */
    public static final String SYSPROPS_MAPPED_PREFIX = "org.xbib.io.sshd.config";

    /**
     * The one and only wrapper instance
     */
    public static final SyspropsMapWrapper INSTANCE = new SyspropsMapWrapper();

    /**
     * A {@link PropertyResolver} with no parent that exposes the system properties
     */
    public static final PropertyResolver SYSPROPS_RESOLVER = new PropertyResolver() {
        @Override
        public Map<String, Object> getProperties() {
            return SyspropsMapWrapper.INSTANCE;
        }

        @Override
        public PropertyResolver getParentPropertyResolver() {
            return null;
        }

        @Override
        public String toString() {
            return "SYSPROPS";
        }
    };

    private SyspropsMapWrapper() {
        super();
    }

    /**
     * @param key Key to be tested
     * @return {@code true} if key starts with {@link #SYSPROPS_MAPPED_PREFIX}
     * and continues with a dot followed by some characters
     */
    public static boolean isMappedSyspropKey(String key) {
        return (GenericUtils.length(key) > (SYSPROPS_MAPPED_PREFIX.length() + 1))
                && key.startsWith(SYSPROPS_MAPPED_PREFIX)
                && (key.charAt(SYSPROPS_MAPPED_PREFIX.length()) == '.');
    }

    /**
     * @param key Key to be transformed
     * @return The &quot;pure&quot; key name if a mapped one, same as input otherwise
     * @see #isMappedSyspropKey(String)
     */
    public static String getUnmappedSyspropKey(Object key) {
        String s = Objects.toString(key);
        return isMappedSyspropKey(s) ? s.substring(SYSPROPS_MAPPED_PREFIX.length() + 1 /* skip dot */) : s;
    }

    /**
     * @param key The original key
     * @return A key prefixed by {@link #SYSPROPS_MAPPED_PREFIX}
     * @see #isMappedSyspropKey(String)
     */
    public static String getMappedSyspropKey(Object key) {
        return SYSPROPS_MAPPED_PREFIX + "." + key;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("sysprops#clear() N/A");
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        // not the most efficient implementation, but we do not expect it to be called much
        Properties props = System.getProperties();
        for (String key : props.stringPropertyNames()) {
            if (!isMappedSyspropKey(key)) {
                continue;
            }

            Object v = props.getProperty(key);
            if (Objects.equals(v, value)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        Properties props = System.getProperties();
        // return a copy in order to avoid concurrent modifications
        Set<Entry<String, Object>> entries =
                new TreeSet<>(Pair.byKeyEntryComparator());
        for (String key : props.stringPropertyNames()) {
            if (!isMappedSyspropKey(key)) {
                continue;
            }

            Object v = props.getProperty(key);
            if (v != null) {
                entries.add(new Pair<>(getUnmappedSyspropKey(key), v));
            }
        }

        return entries;
    }

    @Override
    public Object get(Object key) {
        return (key instanceof String) ? System.getProperty(getMappedSyspropKey(key)) : null;
    }

    @Override
    public boolean isEmpty() {
        return GenericUtils.isEmpty(keySet());
    }

    @Override
    public Set<String> keySet() {
        return System.getProperties()
                .stringPropertyNames().stream()
                // filter out any non-SSHD properties
                .filter(SyspropsMapWrapper::isMappedSyspropKey)
                .map(SyspropsMapWrapper::getUnmappedSyspropKey)
                .collect(Collectors.toSet());
    }

    @Override
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException("sysprops#put(" + key + ")[" + value + "] N/A");
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        throw new UnsupportedOperationException("sysprops#putAll(" + m + ") N/A");
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException("sysprops#remove(" + key + ") N/A");
    }

    @Override
    public int size() {
        return GenericUtils.size(keySet());
    }

    @Override
    public Collection<Object> values() {
        Properties props = System.getProperties();
        // return a copy in order to avoid concurrent modifications
        return props
                .stringPropertyNames().stream()
                .filter(SyspropsMapWrapper::isMappedSyspropKey)
                .map(props::get)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return Objects.toString(System.getProperties(), null);
    }
}

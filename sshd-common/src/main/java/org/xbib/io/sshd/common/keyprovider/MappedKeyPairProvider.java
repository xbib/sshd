package org.xbib.io.sshd.common.keyprovider;

import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;

import java.security.KeyPair;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Holds a {@link Map} of {@link String}-&gt;{@link KeyPair} where the map key
 * is the type and value is the associated {@link KeyPair}
 */
public class MappedKeyPairProvider implements KeyPairProvider {
    /**
     * Transforms a {@link Map} of {@link String}-&gt;{@link KeyPair} to a
     * {@link KeyPairProvider} where map key is the type and value is the
     * associated {@link KeyPair}
     */
    public static final Function<Map<String, KeyPair>, KeyPairProvider> MAP_TO_KEY_PAIR_PROVIDER =
            MappedKeyPairProvider::new;

    private final Map<String, KeyPair> pairsMap;

    public MappedKeyPairProvider(KeyPair... pairs) {
        this(GenericUtils.isEmpty(pairs) ? Collections.emptyList() : Arrays.asList(pairs));
    }

    public MappedKeyPairProvider(Collection<? extends KeyPair> pairs) {
        this(mapUniquePairs(pairs));
    }

    public MappedKeyPairProvider(Map<String, KeyPair> pairsMap) {
        this.pairsMap = ValidateUtils.checkNotNullAndNotEmpty(pairsMap, "No pairs map provided");
    }

    public static Map<String, KeyPair> mapUniquePairs(Collection<? extends KeyPair> pairs) {
        if (GenericUtils.isEmpty(pairs)) {
            return Collections.emptyMap();
        }

        Map<String, KeyPair> pairsMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (KeyPair kp : pairs) {
            String keyType = ValidateUtils.checkNotNullAndNotEmpty(KeyUtils.getKeyType(kp), "Cannot determine key type");
            KeyPair prev = pairsMap.put(keyType, kp);
            ValidateUtils.checkTrue(prev == null, "Multiple keys of type=%s", keyType);
        }

        return pairsMap;
    }

    @Override
    public Iterable<KeyPair> loadKeys() {
        return pairsMap.values();
    }

    @Override
    public KeyPair loadKey(String type) {
        return pairsMap.get(type);
    }

    @Override
    public Iterable<String> getKeyTypes() {
        return pairsMap.keySet();
    }

    @Override
    public String toString() {
        return String.valueOf(getKeyTypes());
    }
}

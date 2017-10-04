package org.xbib.io.sshd.common.config.keys.loader.pem;

import org.xbib.io.sshd.common.config.keys.FilePasswordProvider;
import org.xbib.io.sshd.common.config.keys.loader.KeyPairResourceParser;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public final class PEMResourceParserUtils {
    private static final Map<String, KeyPairPEMResourceParser> BY_OID_MAP = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private static final Map<String, KeyPairPEMResourceParser> BY_ALGORITHM_MAP = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private static final AtomicReference<KeyPairResourceParser> PROXY_HOLDER = new AtomicReference<>(KeyPairResourceParser.EMPTY);
    public static final KeyPairResourceParser PROXY = new KeyPairResourceParser() {
        @Override
        public Collection<KeyPair> loadKeyPairs(
                String resourceKey, FilePasswordProvider passwordProvider, List<String> lines)
                throws IOException, GeneralSecurityException {
            @SuppressWarnings("synthetic-access")
            KeyPairResourceParser proxy = PROXY_HOLDER.get();
            return (proxy == null) ? Collections.<KeyPair>emptyList() : proxy.loadKeyPairs(resourceKey, passwordProvider, lines);
        }

        @Override
        public boolean canExtractKeyPairs(String resourceKey, List<String> lines)
                throws IOException, GeneralSecurityException {
            @SuppressWarnings("synthetic-access")
            KeyPairResourceParser proxy = PROXY_HOLDER.get();
            return (proxy != null) && proxy.canExtractKeyPairs(resourceKey, lines);
        }
    };

    static {
        registerPEMResourceParser(RSAPEMResourceKeyPairParser.INSTANCE);
        registerPEMResourceParser(DSSPEMResourceKeyPairParser.INSTANCE);
        registerPEMResourceParser(ECDSAPEMResourceKeyPairParser.INSTANCE);
        registerPEMResourceParser(PKCS8PEMResourceKeyPairParser.INSTANCE);
    }

    private PEMResourceParserUtils() {
        throw new UnsupportedOperationException("No instance");
    }

    public static void registerPEMResourceParser(KeyPairPEMResourceParser parser) {
        Objects.requireNonNull(parser, "No parser to register");
        synchronized (BY_OID_MAP) {
            BY_OID_MAP.put(ValidateUtils.checkNotNullAndNotEmpty(parser.getAlgorithmIdentifier(), "No OID value"), parser);
        }

        synchronized (BY_ALGORITHM_MAP) {
            BY_ALGORITHM_MAP.put(ValidateUtils.checkNotNullAndNotEmpty(parser.getAlgorithm(), "No algorithm value"), parser);
            // Use a copy in order to avoid concurrent modifications
            PROXY_HOLDER.set(KeyPairResourceParser.aggregate(new ArrayList<>(BY_ALGORITHM_MAP.values())));
        }
    }

    public static KeyPairPEMResourceParser getPEMResourceParserByOid(String oid) {
        if (GenericUtils.isEmpty(oid)) {
            return null;
        }

        synchronized (BY_OID_MAP) {
            return BY_OID_MAP.get(oid);
        }
    }

    public static KeyPairPEMResourceParser getPEMResourceParserByAlgorithm(String algorithm) {
        if (GenericUtils.isEmpty(algorithm)) {
            return null;
        }

        synchronized (BY_ALGORITHM_MAP) {
            return BY_ALGORITHM_MAP.get(algorithm);
        }
    }
}
